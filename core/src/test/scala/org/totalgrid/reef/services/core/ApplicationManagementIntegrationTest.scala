/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.proto.ProcessStatus._

import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.proto.Application.{ ApplicationConfig, HeartbeatConfig }

import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.sapi.client.Event
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry

import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.models.DatabaseUsingTestBaseNoTransaction
import org.totalgrid.reef.services.{ DependenciesSource, ServiceDependencies, ServiceBootstrap }
import org.totalgrid.reef.services.framework.ServiceMiddleware

@RunWith(classOf[JUnitRunner])
class ApplicationManagementIntegrationTest extends DatabaseUsingTestBaseNoTransaction {

  override def beforeEach() {
    ServiceBootstrap.resetDb
  }

  class Fixture(amqp: AMQPProtoFactory) {

    val start = System.currentTimeMillis

    val deps = ServiceDependencies(new ServiceEventPublisherRegistry(amqp, ReefServicesList))
    val contextSource = new DependenciesSource(deps)

    val modelFac = new ModelFactories(deps)

    val processStatusService = new ServiceMiddleware(contextSource, new ProcessStatusService(modelFac.procStatus))

    val applicationConfigService = new ServiceMiddleware(contextSource, new ApplicationConfigService(modelFac.appConfig))

    val processStatusCoordinator = new ProcessStatusCoordinator(modelFac.procStatus, contextSource)

    amqp.bindService(applicationConfigService.descriptor.id, applicationConfigService.respond)
    amqp.bindService(processStatusService.descriptor.id, processStatusService.respond)

    val client = amqp.getProtoClientSession(ReefServicesList, 5000)

    /// current state of the StatusSnapshot
    var lastSnapShot = new SyncVar[Option[StatusSnapshot]](None: Option[StatusSnapshot])

    /// register the application with the services handler
    val appConfig = registerInstance()

    /// use the appConfig information to setup the heartbeat publisher
    val hbeatSink = processStatusCoordinator.handleRawStatus _

    /// setup the subscription to the Snapshot service so we track the current status of the application
    subscribeSnapshotStatus()

    def registerInstance(): ApplicationConfig = {
      val b = ApplicationConfig.newBuilder()
      b.setUserName("proc").setInstanceName("proc01").setNetwork("any").setLocation("farm1").addCapabilites("Processing")
      b.setHeartbeatCfg(HeartbeatConfig.newBuilder.setPeriodMs(100)) // override the default period
      client.put(b.build).await().expectOne()
    }

    private def subscribeSnapshotStatus() {
      val eventQueueName = new SyncVar("")
      val hbeatSource = amqp.getEventQueue(StatusSnapshot.parseFrom, { evt: Event[StatusSnapshot] => lastSnapShot.update(Some(evt.value)) }, { q => eventQueueName.update(q) })

      // wait for the queue name to get populated (actor srtup delay)
      eventQueueName.waitWhile("")

      val env = new RequestEnv
      env.setSubscribeQueue(eventQueueName.current)
      val config = client.get(StatusSnapshot.newBuilder.setInstanceName(appConfig.getInstanceName).build, env).await().expectOne()
      // do some basic checks to make sure we got the correct initial state
      config.getInstanceName should equal(appConfig.getInstanceName)
      config.getOnline should equal(true)

      lastSnapShot.update(Some(config))
    }

    /// wait up to 5 seconds for the condition to be satisfied (for actor messaging delays)
    def waitUntilSnapshot(f: StatusSnapshot => Boolean): Boolean = {
      val wrap = { o: Option[StatusSnapshot] => if (o.isDefined) f(o.get) else false }
      if (wrap(lastSnapShot.current)) return true
      lastSnapShot.waitFor(wrap)
    }

    /// simulate doing sending a heartbeat from the application
    def doHeartBeat(online: Boolean, time: Long) {
      val msg = StatusSnapshot.newBuilder
        .setProcessId(appConfig.getHeartbeatCfg.getProcessId)
        .setInstanceName(appConfig.getInstanceName)
        .setTime(time)
        .setOnline(online).build
      hbeatSink(msg)
    }

    def checkTimeouts(time: Long) = processStatusCoordinator.checkTimeouts(time)

  }

  test("Application Timesout") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.checkTimeouts(fix.start + 1000000)

      fix.waitUntilSnapshot(_.getOnline == false)
    }
  }

  test("Application Stays Online w/ Heartbeats") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.checkTimeouts(fix.start + 1)
      fix.waitUntilSnapshot(_.getOnline == true)

      fix.doHeartBeat(true, fix.start + 1090)
      fix.checkTimeouts(fix.start + 1100)
      fix.waitUntilSnapshot(_.getOnline == true)

      fix.doHeartBeat(true, fix.start + 1190)
      fix.checkTimeouts(fix.start + 1200)
      fix.waitUntilSnapshot(_.getOnline == true)
    }
  }

  test("Application can go offline cleanly") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.doHeartBeat(false, fix.start + 200)
      fix.waitUntilSnapshot(_.getOnline == false)
    }
  }

  test("Application can go online/offline") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.doHeartBeat(false, fix.start + 200)
      fix.waitUntilSnapshot(_.getOnline == false)

      fix.registerInstance()
      fix.waitUntilSnapshot(_.getOnline == true)

      fix.doHeartBeat(false, fix.start + 5000)
      fix.waitUntilSnapshot(_.getOnline == false)

      fix.registerInstance()
      fix.waitUntilSnapshot(_.getOnline == true)
    }
  }

}
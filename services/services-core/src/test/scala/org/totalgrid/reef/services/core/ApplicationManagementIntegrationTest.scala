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
import org.totalgrid.reef.client.service.proto.ProcessStatus._

import org.totalgrid.reef.client.service.proto.Application.{ ApplicationConfig, HeartbeatConfig }

import org.totalgrid.reef.client.operations.scl.Event

import org.totalgrid.reef.client.Connection
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.services.ConnectionFixture
import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.models.{ CoreServicesSchema, DatabaseUsingTestNotTransactionSafe }

import org.totalgrid.reef.client.sapi.client.Expectations._

@RunWith(classOf[JUnitRunner])
class ApplicationManagementIntegrationTest extends DatabaseUsingTestNotTransactionSafe {

  override def beforeEach() {
    CoreServicesSchema.prepareDatabase(dbConnection)
  }

  class Fixture(conn: Connection) {

    val start = System.currentTimeMillis

    val deps = new ServiceDependenciesDefaults(dbConnection, conn, conn.getServiceRegistration.getEventPublisher)
    val contextSource = new MockRequestContextSource(deps)

    val modelFac = new ModelFactories(deps)

    val processStatusService = new SyncService(new ProcessStatusService(modelFac.procStatus, false), contextSource)

    val applicationConfigService = new SyncService(new ApplicationConfigService(modelFac.appConfig), contextSource)

    val processStatusCoordinator = new ProcessStatusCoordinator(modelFac.procStatus, contextSource)

    val client = conn.createClient("fakeAuth")

    /// current state of the StatusSnapshot
    var lastSnapShot = new SyncVar[Option[StatusSnapshot]](None: Option[StatusSnapshot])

    var appConfig: ApplicationConfig = null
    /// register the application with the services handler
    registerInstance()

    /// use the appConfig information to setup the heartbeat publisher
    val hbeatSink = { hbeat: StatusSnapshot => processStatusService.put(hbeat).expectOne }

    /// setup the subscription to the Snapshot service so we track the current status of the application
    subscribeSnapshotStatus()

    def registerInstance(): ApplicationConfig = {
      val b = ApplicationConfig.newBuilder()
      b.setUserName("proc").setInstanceName("proc01").setNetwork("any").setLocation("farm1").addCapabilites("Processing")
      b.setHeartbeatCfg(HeartbeatConfig.newBuilder.setPeriodMs(100)) // override the default period
      appConfig = applicationConfigService.put(b.build).expectOne
      appConfig
    }

    private def subscribeSnapshotStatus() {

      val env = getSubscriptionQueue(client, Descriptors.statusSnapshot, { evt: Event[StatusSnapshot] => lastSnapShot.update(Some(evt.value)) })

      val config = processStatusService.get(StatusSnapshot.newBuilder.setInstanceName(appConfig.getInstanceName).build, env).expectOne()
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
    ConnectionFixture.mock() { conn =>
      val fix = new Fixture(conn)

      fix.checkTimeouts(fix.start + 1000000)

      fix.waitUntilSnapshot(_.getOnline == false)
    }
  }

  test("Application Stays Online w/ Heartbeats") {
    ConnectionFixture.mock() { conn =>
      val fix = new Fixture(conn)

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
    ConnectionFixture.mock() { conn =>
      val fix = new Fixture(conn)

      fix.doHeartBeat(false, fix.start + 200)
      fix.waitUntilSnapshot(_.getOnline == false)
    }
  }

  test("Application can go online/offline") {
    ConnectionFixture.mock() { conn =>
      val fix = new Fixture(conn)

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
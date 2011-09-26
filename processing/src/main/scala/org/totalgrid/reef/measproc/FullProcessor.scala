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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.executor.{ Lifecycle, LifecycleManager }
import org.totalgrid.reef.proto.Measurements._
import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection => ConnProto }

import org.totalgrid.reef.executor.ReactActorExecutor
import org.totalgrid.reef.app.{ CoreApplicationComponents }
import org.totalgrid.reef.persistence.{ InMemoryObjectCache }
import org.totalgrid.reef.measurementstore.{ MeasurementStore, MeasurementStoreToMeasurementCacheAdapter }

/**
 *  Non-entry point meas processor setup
 */
class FullProcessor(components: CoreApplicationComponents, measStore: MeasurementStore) extends Logging with Lifecycle {

  var lifecycles = new LifecycleManager(List(components.heartbeatActor))

  // caches used to store measurements and overrides
  val measCache = new MeasurementStoreToMeasurementCacheAdapter(measStore)

  // TODO: make override caches configurable like measurement store

  val overCache = new InMemoryObjectCache[Measurement]
  val triggerStateCache = new InMemoryObjectCache[Boolean]

  val connectionHandler = new ConnectionHandler(addStreamProcessor(_)) with ReactActorExecutor

  override def doStart() {
    lifecycles.start
    subscribeToStreams
  }

  override def doStop() {
    // make sure we stop the meas proc from readding connections
    // as we are shutting down
    connectionHandler.running = false
    connectionHandler.clear
    lifecycles.stop
  }

  def addStreamProcessor(streamConfig: ConnProto): MeasurementStreamProcessingNode = {
    val reactor = new ReactActorExecutor {}
    val streamHandler = new MeasurementStreamProcessingNode(components.amqp, components.registry, measCache, overCache, triggerStateCache, streamConfig, reactor)
    streamHandler.setHookSource(components.metricsPublisher.getStore("measproc-" + streamConfig.getLogicalNode.getName))
    streamHandler
  }

  def subscribeToStreams() = {
    val connection = ConnProto.newBuilder.setMeasProc(components.appConfig).build
    connectionHandler.serviceHandler.addServiceContext(components.registry, 5000, ConnProto.parseFrom, connection, connectionHandler)
    connectionHandler.start
  }
}


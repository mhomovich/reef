/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.rpc.impl.builders

import org.totalgrid.reef.client.service.proto.Alarms.Alarm
import org.totalgrid.reef.client.service.proto.Alarms.EventConfig

object EventConfigRequestBuilders {
  private def makePartialEventConfig(eventTypeName: String, resource: String, severity: Int) = {
    EventConfig.newBuilder.setEventType(eventTypeName).setResource(resource).setSeverity(severity)
  }

  def makeLog(eventTypeName: String, resource: String, severity: Int) = {
    makePartialEventConfig(eventTypeName, resource, severity).setDesignation(EventConfig.Designation.LOG).build
  }

  def makeEvent(eventTypeName: String, resource: String, severity: Int) = {
    makePartialEventConfig(eventTypeName, resource, severity).setDesignation(EventConfig.Designation.EVENT).build
  }

  def makeAudibleAlarm(eventTypeName: String, resource: String, severity: Int) = {
    makePartialEventConfig(eventTypeName, resource, severity).setDesignation(EventConfig.Designation.ALARM).setAlarmState(Alarm.State.UNACK_AUDIBLE).build
  }

  def makeSilentAlarm(eventTypeName: String, resource: String, severity: Int) = {
    makePartialEventConfig(eventTypeName, resource, severity).setDesignation(EventConfig.Designation.ALARM).setAlarmState(Alarm.State.UNACK_SILENT).build
  }
}
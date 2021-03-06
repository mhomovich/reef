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
package org.totalgrid.reef.models

import java.util.UUID
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }
import org.totalgrid.reef.client.service.proto.OptionalProtos.OptModelReefUUID

object UUIDConversions {

  implicit def convertReefUUIDToUUID(optUUID: Option[ReefUUID]) = {
    optUUID.map { ru => UUID.fromString(ru.getValue) }
  }

  implicit def convertReefUUIDToListUUID(optUUID: List[ReefUUID]) = {
    optUUID.map { ru => UUID.fromString(ru.getValue) }
  }

  implicit def convertOptReefUUIDToUUID(optUUID: OptModelReefUUID) = {
    optUUID.value.map { ru => UUID.fromString(ru) }
  }

  implicit def convertUUIDtoReefUUID(uuid: UUID) = {
    ReefUUID.newBuilder.setValue(uuid.toString).build
  }

  import org.totalgrid.reef.client.service.proto.Model.ReefUUID
  def makeId(entry: ModelWithId) = {
    ReefID.newBuilder.setValue(entry.id.toString)
  }
  def makeUuid(entry: EntityBasedModel) = {
    ReefUUID.newBuilder.setValue(entry.entityId.toString)
  }
  def makeUuid(entry: ModelWithUUID) = {
    ReefUUID.newBuilder.setValue(entry.id.toString)
  }
  def makeUuid(id: Long) = {
    ReefUUID.newBuilder.setValue(id.toString)
  }
  def makeUuid(id: java.util.UUID) = {
    ReefUUID.newBuilder.setValue(id.toString)
  }
}
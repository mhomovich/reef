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
package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }

object CalcLibTestHelpers {

  def makeTraceMeas(v: Int, time: Long = 0) = {
    Measurement.newBuilder()
      .setName("test01")
      .setType(Measurement.Type.INT)
      .setIntVal(v)
      .setQuality(Quality.newBuilder)
      .setUnit("u")
      .setTime(time)
      .build
  }

  class MockTimeSource(var time: Long = 0) extends TimeSource {

    def now = time
  }
}

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.util.Logging
import scala.collection.Map

/** Shim layer to push log messages and non-operational logvars to the bus from the c++ dnp3 world
 * @param nonOpPub c++ LogVars are translated to non-operational data.
 * @param enumToString Map that defines which log variable names are string enums and what that mapping is
 */
class LogAdapter(enumToString: Map[String, Map[Int, String]]) extends ILogBase with Logging {

  override def Log(lev: FilterLevel, logger: String, location: String, message: String, code: Int): Unit = {
    lev match {
      case FilterLevel.LEV_COMM => debug { message }
      case FilterLevel.LEV_DEBUG => debug { message }
      case FilterLevel.LEV_ERROR => error { message }
      case FilterLevel.LEV_INFO => info { message }
      case FilterLevel.LEV_INTERPRET => info { message }
      case FilterLevel.LEV_WARNING => warn { message }
    }
  }

}
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
package org.totalgrid.reef.models.liquibaseschema

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models.CoreServicesSchema._
import org.totalgrid.reef.measurementstore.squeryl.SqlMeasurementStoreSchema
import org.totalgrid.reef.models.{ CoreServicesSchema, ApplicationSchema }

@RunWith(classOf[JUnitRunner])
class MigrationTests extends FunSuite {

  test("Migrate onto existing db") {

    val baseInfo = DbInfo.loadInfo("../../org.totalgrid.reef.test.cfg")
    val dbConnection = DbConnector.connect(baseInfo)

    useDb(dbConnection) { referenceDb =>
      clearDatabase(referenceDb)
    }
    dbConnection.transaction {
      ApplicationSchema.reset()
      SqlMeasurementStoreSchema.reset()
    }
    intercept[CoreServicesSchema.FirstMigrationNeededException] {
      CoreServicesSchema.prepareDatabase(dbConnection, false, true)
    }
    CoreServicesSchema.prepareDatabase(dbConnection, true, true)

    CoreServicesSchema.prepareDatabase(dbConnection, false, true)
  }
}

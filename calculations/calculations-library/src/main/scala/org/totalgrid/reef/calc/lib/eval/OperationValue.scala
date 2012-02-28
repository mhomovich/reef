package org.totalgrid.reef.calc.lib.eval
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

sealed trait OperationValue {
  def toList: List[OperationValue] = List(this)
}

case class ValueRange(list: List[OperationValue]) extends OperationValue {
  override def toList: List[OperationValue] = list
}

trait NumericValue extends OperationValue {
  val value: Double
}
object NumericValue {
  def unapply(v: NumericValue): Option[Double] = Some(v.value)
}

case class NumericConst(value: Double) extends NumericValue

case class BooleanConst(value: Boolean) extends OperationValue

case class NumericMeas(value: Double, time: Long) extends NumericValue

case class BooleanMeas(value: Boolean, time: Long) extends OperationValue

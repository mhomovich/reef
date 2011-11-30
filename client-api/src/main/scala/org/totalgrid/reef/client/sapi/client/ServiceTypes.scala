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
package org.totalgrid.reef.client.sapi.client

import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.client.SubscriptionEvent
import org.totalgrid.reef.client.proto.{ StatusCodes, Envelope }
import org.totalgrid.reef.client.exceptions.ExpectationException

/* ---- Case classes that make the service api easier to use ---- */

case class Request[+A](verb: Envelope.Verb, payload: A, env: BasicRequestHeaders = BasicRequestHeaders.empty)

object Response {

  def convert[A](option: Option[Response[A]]): Response[A] = option match {
    case Some(x) => x
    case None => ResponseTimeout
  }

  def apply[A](status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, list: List[A] = Nil, error: String = ""): Response[A] = {
    if (StatusCodes.isSuccess(status)) SuccessResponse(status, list)
    else FailureResponse(status, error)
  }

  def apply[A](status: Envelope.Status, single: A): Response[A] = apply(status, single :: Nil, "")
}

sealed trait Response[+A] extends Expectations[A] {

  def status: Envelope.Status
  def list: List[A]
  def error: String
  def success: Boolean

  final override def expectMany(num: Option[Int], expected: Option[Envelope.Status], errorFun: Option[(Int, Int) => String]): List[A] = {

    expected match {
      case Some(x) =>
        if (status != x)
          throw new ExpectationException("Status " + status + " != " + " expected " + x)
        list
      case None => this match {
        case SuccessResponse(_, list) => list
        case FailureResponse(status, error) => throw StatusCodes.toException(status, error)
      }
    }

    num.foreach { expected =>
      val actual = list.size
      if (expected != actual) {
        val msg = errorFun match {
          case Some(fun) => fun(expected, actual)
          case None => defaultError(expected, actual)
        }
        throw new ExpectationException(msg)
      }
    }

    list
  }

}

final case class SuccessResponse[A](status: Envelope.Status = Envelope.Status.OK, list: List[A])
    extends Response[A] {

  override val error = ""
  override val success = true
  override def many = Success(list)
}

sealed case class FailureResponse(status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, error: String = "")
    extends Response[Nothing] {

  override val list = Nil
  override val success = false
  override def many = Failure(StatusCodes.toException(status, error))
  override def toString = "Request failed with status: " + status + ", msg: " + error
}

object ResponseTimeout extends FailureResponse(Envelope.Status.RESPONSE_TIMEOUT)

case class Event[A](event: Envelope.SubscriptionEventType, value: A) extends SubscriptionEvent[A] {

  final override def getEventType() = event
  final override def getValue() = value
}

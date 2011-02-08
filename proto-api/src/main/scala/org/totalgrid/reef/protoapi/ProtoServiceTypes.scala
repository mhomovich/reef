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
package org.totalgrid.reef.protoapi

import org.totalgrid.reef.proto.Envelope

/**Contains types/case classes used in the proto service interfaces
 */
object ProtoServiceTypes {

  /**
   * Convert a response option into a MultiResult
   */
  def convert[A](response: Option[Response[A]]): MultiResult[A] = response match {
    case Some(Response(status, msg, list)) =>
      if (StatusCodes.isSuccess(status)) MultiSuccess(list)
      else Failure(status, msg)
    case None => Failure(Envelope.Status.RESPONSE_TIMEOUT, "Service response timeout")
  }

  def expectsOne[A](response: MultiResult[A]): SingleResult[A] = response match {
    case MultiSuccess(List(x)) => SingleSuccess(x)
    case MultiSuccess(list) =>
      Failure(Envelope.Status.UNEXPECTED_RESPONSE, "Expected one result, but got: " + list.size)
    case x: Failure => x
  }

  /* ---- Case classes that make the service protoapi easier to use ---- */

  case class Request[A](verb: Envelope.Verb, payload: A, env: RequestEnv)

  case class Response[A](status: Envelope.Status, error: String, result: List[A]) {
    def this(status: Envelope.Status, result: List[A]) = this(status, "", result)

    def this(status: Envelope.Status, result: A) = this(status, "", List(result))
  }

  case class Event[A](event: Envelope.Event, result: A) {
    // accessors for java client
    def getEvent() = event

    def getResult() = result
  }

  /* ---- Further decomposition that makes matching results even easier to use ---- */

  trait MultiResult[+A]

  trait SingleResult[+A]

  case class SingleSuccess[A](result: A) extends SingleResult[A]

  case class MultiSuccess[A](result: List[A]) extends MultiResult[A]

  case class Failure(status: Envelope.Status, error: String = "") extends Throwable with SingleResult[Nothing] with MultiResult[Nothing] {
    override def toString: String = super.toString + " " + status + " message: " + error

    def toException: ProtoServiceException = new ProtoServiceException(error, status)
  }

}
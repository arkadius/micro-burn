/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.util.concurrent

import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.{Full, Failure}
import net.liftweb.util.Schedule
import org.github.microburn.util.logging.Slf4jLogging
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

trait JobRepeatingActor extends LiftActor { self: Slf4jLogging =>
  import org.github.microburn.util.concurrent.FutureEnrichments._

  protected def tickPeriod: FiniteDuration
  protected def jobDescription: String

  def start(): Unit = this ! Repeat

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case Repeat =>
      this ! Tick(new DateTime())
    case Tick(timestamp) =>
      try {
        prepareFutureOfJob(timestamp).onComplete { result =>
          result match {
            case Failure(msg, Full(ex), _) => error(s"Error while $jobDescription: $msg", ex)
            case Failure(msg, _, _) => error(s"Error while $jobDescription: $msg")
            case _ =>
          }
          Schedule.schedule(this, Repeat, tickPeriod.toTimeSpan)
        }
      } catch {
        case NonFatal(ex) =>
          error(s"Error while preparation of $jobDescription", ex)
          Schedule.schedule(this, Repeat, tickPeriod.toTimeSpan)
      }
  }

  protected def prepareFutureOfJob(timestamp: DateTime): LAFuture[_]

  case object Repeat

  case class Tick(dateTime: DateTime)

}
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
package org.github.microburn.util.date

import net.liftweb.common.{Failure, Box}

import scalaz.Validation

case class Time(hour: Int, minute: Int)

object TimeParser {
  import org.github.microburn.util.validation.ValidationBuilder._

  import scalaz.Scalaz._

  def parse(str: String): Validation[String, Time] = {
    val parts = str.split(":")
    for {
      _ <- "Time should be in format: HH:mm" failureIfNot parts.length == 2
      hourStr = parts(0)
      hour <- hourStr.parseInt.leftMap(_.getMessage)
      _ <- "Hour should be between 0 and 23" failureIfNot 0 <= hour && hour <= 23
      minuteStr = parts(1)
      minute <- minuteStr.parseInt.leftMap(_.getMessage)
      _ <- "Minute should be between 0 and 59" failureIfNot 0 <= hour && hour <= 59
    } yield Time(hour, minute)
  }
}
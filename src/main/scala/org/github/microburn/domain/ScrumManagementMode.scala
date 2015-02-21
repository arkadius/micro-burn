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
package org.github.microburn.domain

import java.text.ParseException

import com.typesafe.config.Config
import org.github.microburn.util.date.Time
import org.joda.time.{DateTime, DateTimeConstants}

sealed trait ScrumManagementMode

case object ManualManagementMode extends ScrumManagementMode

case class AutomaticManagementMode(restartPeriod: RepeatPeriod) extends ScrumManagementMode

sealed trait RepeatPeriod {
  def n: Int
  def time: Time
  def optionalStartDate: Option[DateTime]
}

case class EveryNDays(n: Int, time: Time, optionalStartDate: Option[DateTime]) extends RepeatPeriod

case class EveryNWeeks(n: Int, dayOfWeek: Int, time: Time, optionalStartDate: Option[DateTime]) extends RepeatPeriod

case class EveryNMonths(n: Int, dayOfMonth: Int, time: Time, optionalStartDate: Option[DateTime]) extends RepeatPeriod

object ScrumManagementModeParser {
  import org.github.microburn.util.config.ConfigEnrichments._

  def parse(config: Config): ScrumManagementMode = {
    val managementConfig = config.getConfig("management")
    managementConfig.getString("mode") match {
      case "manual" => ManualManagementMode
      case "auto" => parseAutomatic(managementConfig)
      case otherMode => throw new ParseException(s"Invalid mode: $otherMode. Valid are: auto and manual", -1)
    }
  }

  private def parseAutomatic(config: Config): ScrumManagementMode = {
    val n = config.optional(_.getInt, "n").getOrElse(1)
    require(1 <= n && n <= 365, "n should be between 1 and 365")
    val time = config.optional(_.getTime, "time").getOrElse(Time(0, 0))
    val optionalStartDate = config.optional(_.getDate, "start-date")
    val period = config.getString("period") match {
      case "every-n-days" =>
        EveryNDays(n, time, optionalStartDate)
      case "every-n-weeks" =>
        val dayOfWeek = config.optional(_.getDayOfWeek, "day-of-week").getOrElse(DateTimeConstants.MONDAY)
        EveryNWeeks(n, dayOfWeek, time, optionalStartDate)
      case "every-n-months" =>
        val dayOfMonth = config.optional(_.getDayOfMonth, "day-of-month").getOrElse(1)
        EveryNMonths(n, dayOfMonth, time, optionalStartDate)
      case otherPeriodType =>
        throw new ParseException(s"Illegal period type: $otherPeriodType", -1)
    }
    AutomaticManagementMode(period)
  }
}
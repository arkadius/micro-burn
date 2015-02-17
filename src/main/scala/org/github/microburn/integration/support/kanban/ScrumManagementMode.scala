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
package org.github.microburn.integration.support.kanban

import java.text.ParseException

import com.typesafe.config.Config
import org.joda.time.DateTime

sealed trait ScrumManagementMode {
  def automaticScopeChange: Boolean
}

case class ManualManagementMode(automaticScopeChange: Boolean) extends ScrumManagementMode

case class AutomaticManagementMode(restartPeriod: RepeatPeriod) extends ScrumManagementMode {
  override def automaticScopeChange: Boolean = true
}

sealed trait RepeatPeriod

case class EveryNDays(n: Int, hour: Int, minute: Int, optionalStartDate: Option[DateTime]) extends RepeatPeriod

case class EveryNWeeks(n: Int, dayOfWeek: Int, hour: Int, minute: Int, optionalStartDate: Option[DateTime]) extends RepeatPeriod

case class EveryNMonths(n: Int, dayOfMonth: Int, hour: Int, minute: Int, optionalStartDate: Option[DateTime]) extends RepeatPeriod

object ScrumManagementModeParser {
  import org.github.microburn.util.config.ConfigExtensions._

  def parse(config: Config): Option[ScrumManagementMode] = {
    config.optional(_.getConfig, "management").map { managementConfig =>
      managementConfig.getString("mode") match {
        case "manual" => parseManual(managementConfig)
        case "auto" => parseAutomatic(managementConfig)
        case otherMode => throw new ParseException(s"Invalid mode: $otherMode. Valid are: auto and manual", -1)
      }
    }
  }

  private def parseManual(config: Config): ScrumManagementMode = {
    val automaticScopeChange = config.optional(_.getBoolean, "automaticScopeChange").getOrElse(false)
    ManualManagementMode(automaticScopeChange)
  }

  private def parseAutomatic(config: Config): ScrumManagementMode = {
    val n = config.optional(_.getInt, "n").getOrElse(1)
    require(1 <= n && n <= 365, "n should be between 1 and 365")
    val hour = config.optional(_.getInt, "hour").getOrElse(0)
    require(0 <= hour && n <= 23, "hour should be between 0 and 23")
    val minute = config.optional(_.getInt, "minute").getOrElse(0)
    require(0 <= hour && n <= 59, "minute should be between 0 and 59")
    val optionalStartDate = config.optional(_.getDateTime, "start-date")
    val period = config.getString("period") match {
      case "every-n-days" =>
        EveryNDays(n, hour, minute, optionalStartDate)
      case "every-n-weeks" =>
        val dayOfWeek = config.getDayOfWeek("day-of-week")
        EveryNWeeks(n, dayOfWeek, hour, minute, optionalStartDate)
      case "every-n-months" =>
        val dayOfMonth = config.getInt("day-of-month")
        require(1 <= dayOfMonth && dayOfMonth <= 31, "day-of-month should be between 1 and 31")
        EveryNMonths(n, dayOfMonth, hour, minute, optionalStartDate)
      case otherPeriodType =>
        throw new ParseException(s"Illegal period type: $otherPeriodType", -1)
    }
    AutomaticManagementMode(period)
  }
}
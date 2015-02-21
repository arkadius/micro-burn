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

import org.github.microburn.domain.{EveryNMonths, EveryNWeeks, EveryNDays, RepeatPeriod}
import org.github.microburn.util.date.DateMath
import org.joda.time.DateTime

class NextRestartComputer(restartPeriod: RepeatPeriod) {
  import org.github.microburn.util.date.DateTimeEnrichments._

  def compute(optionalLastRestart: Option[DateTime], currentDate: DateTime): NextRestart = {
    val nextDate = optionalLastRestart match {
      case None =>
        nextAfterStarOrCurrent(currentDate)
      case Some(lastRestart) =>
        val next = restartPeriod match {
          case days: EveryNDays =>
            lastRestart.plusDays(days.n).withTime(restartPeriod.time)
          case weeks: EveryNWeeks =>
            lastRestart.plusWeeks(weeks.n).withDayOfWeek(weeks.dayOfWeek).withTime(restartPeriod.time)
          case months: EveryNMonths =>
            lastRestart.plusMonths(months.n).withDayOfMonth(months.dayOfMonth).withTime(restartPeriod.time)
        }
        val maxOfNextAndCurrent = DateMath.maxOfDates(next, currentDate)
        if (restartPeriod.optionalStartDate.exists(maxOfNextAndCurrent.isBefore(_)))
          nextAfterStarOrCurrent(currentDate)
        else
          maxOfNextAndCurrent
    }
    NextRestart(nextDate, periodName(nextDate))
  }

  private def nextAfterStarOrCurrent(currentDate: DateTime): DateTime = {
    val dates = restartPeriod.optionalStartDate.toSeq :+ currentDate
    val maxOfStartDateAndCurrent = DateMath.maxOfDates(dates: _*)
    restartPeriod match {
      case days: EveryNDays =>
        maxOfStartDateAndCurrent.withFieldsSettedUpButNotBefore(_.withTime(restartPeriod.time), _.plusDays(1))
      case weeks: EveryNWeeks =>
        maxOfStartDateAndCurrent.withFieldsSettedUpButNotBefore(_.withDayOfWeek(weeks.dayOfWeek).withTime(restartPeriod.time), _.plusWeeks(1))
      case months: EveryNMonths =>
        maxOfStartDateAndCurrent.withFieldsSettedUpButNotBefore(_.withDayOfMonth(months.dayOfMonth).withTime(restartPeriod.time), _.plusMonths(1))
    }
  }

  private def periodName(date: DateTime): String = restartPeriod match {
    case days: EveryNDays =>
      date.getYear + "." + date.getDayOfYear.formatted("%03d")
    case weeks: EveryNWeeks =>
      date.getWeekyear + "." + date.getWeekOfWeekyear.formatted("%02d")
    case months: EveryNMonths =>
      date.getYear + "." + date.getMonthOfYear.formatted("%02d")
  }

}

case class NextRestart(date: DateTime, periodName: String)
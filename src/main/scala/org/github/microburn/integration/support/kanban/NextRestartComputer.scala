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

import org.github.microburn.util.date.DateMath
import org.joda.time.DateTime

class NextRestartComputer(restartPeriod: RepeatPeriod, currentDate: => DateTime = new DateTime()) {
  import org.github.microburn.util.date.DateTimeEnrichments._

  def compute(optionalLastRestart: Option[DateTime]): NextRestart = {
    val nextDate = optionalLastRestart match {
      case None =>
        val dates = currentDate +: restartPeriod.optionalStartDate.toSeq
        DateMath.maxOfDates(dates : _*).withTimeButNotBefore(restartPeriod.time)
      case Some(lastRestart) =>
        restartPeriod match {
          case days: EveryNDays =>
           ???
          case weeks: EveryNWeeks =>
            ???
          case months: EveryNMonths =>
            ???
        }
    }
    NextRestart(nextDate, periodName(nextDate))
  }

  private def periodName(date: DateTime): String = restartPeriod match {
    case days: EveryNDays =>
      date.getYear + "." + date.getDayOfYear.formatted("%03d")
    case weeks: EveryNWeeks =>
      date.getWeekyear + "." + date.getWeekOfWeekyear.formatted("%2d")
    case months: EveryNMonths =>
      date.getYear + "." + date.getMonthOfYear.formatted("%2d")
  }

}

case class NextRestart(date: DateTime, periodName: String)
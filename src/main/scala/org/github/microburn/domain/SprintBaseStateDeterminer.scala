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

import java.util.Date

import com.typesafe.config.Config
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

class SprintBaseStateDeterminer(initialFetchToSprintStartAcceptableDelayMinutes: FiniteDuration, baseDetermineMode: SprintBaseDetermineMode) {

  def baseForSprint(details: SprintDetails,
                    initialDate: Date,
                    initialStoryPointsSum: BigDecimal,
                    initialDoneTasksStoryPointsSum: BigDecimal) = {
    val initialAfterStartPlusAcceptableDelay = initialAfterStartPlusDelay(initialDate, details.start)
    val baseForStart = details.overriddenBaseStoryPointsSum getOrElse (baseDetermineMode match {
      case SpecifiedBaseMode(storyPoints) =>
        storyPoints
      case AutomaticOnSprintStartMode =>
        computeBaseForStart(initialStoryPointsSum, initialDoneTasksStoryPointsSum, initialAfterStartPlusAcceptableDelay)
      case AutomaticOnScopeChangeMode =>
        ??? // FIXME
    })

    val baseForColumnChanges = computeBaseForColumnChanges(initialDoneTasksStoryPointsSum, initialAfterStartPlusAcceptableDelay, baseForStart)
    SprintBase(initialAfterStartPlusAcceptableDelay, baseForStart, baseForColumnChanges)
  }

  private def computeBaseForStart(initialStoryPointsSum: BigDecimal, initialDoneTasksStoryPointsSum: BigDecimal, initialAfterStartPlusAcceptableDelay: Boolean): BigDecimal = {
    if (initialAfterStartPlusAcceptableDelay) {
      initialStoryPointsSum
    } else {
      initialStoryPointsSum - initialDoneTasksStoryPointsSum
    }
  }

  private def computeBaseForColumnChanges(initialDoneTasksStoryPointsSum: BigDecimal, initialAfterStartPlusAcceptableDelay: Boolean, baseForStart: BigDecimal): BigDecimal = {
    if (initialAfterStartPlusAcceptableDelay) {
      baseForStart
    } else {
      baseForStart + initialDoneTasksStoryPointsSum
    }
  }

  private def initialAfterStartPlusDelay(initial: Date, startDate: Date): Boolean = {
    val initialDate = new DateTime(initial)
    val startDatePlusAcceptableDelay =
      new DateTime(startDate).plusMillis(initialFetchToSprintStartAcceptableDelayMinutes.toMillis.toInt)
    initialDate.isAfter(startDatePlusAcceptableDelay)
  }
}

case class SprintBase(initialAfterStartPlusAcceptableDelay: Boolean,
                      baseStoryPointsForStart: BigDecimal,
                      baseStoryPointsForColumnChanges: BigDecimal)
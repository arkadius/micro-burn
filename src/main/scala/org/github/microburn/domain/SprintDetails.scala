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
import net.liftweb.common._

import scalaz.Scalaz._

case class SprintDetails private(name: String,
                                 start: Date,
                                 end: Date,
                                 state: SprintState,
                                 overriddenBaseStoryPointsSum: Option[BigDecimal]) {

  def isActive = state == ActiveState

  def isFinished = state == FinishedState

  def isRemoved = state == RemovedState

  def finish: Box[SprintDetails] = verifyAndMoveTo(FinishedState)

  def markRemoved: Box[SprintDetails] = verifyAndMoveTo(RemovedState)

  private def verifyAndMoveTo(newState: SprintState): Box[SprintDetails] = {
    if (!state.canMoveTo(newState))
      Failure(s"Cannot change state from $state to $newState for sprint '$name'")
    else
      Full(copy(state = newState))
  }

  def defineBaseStoryPoints(base: BigDecimal): Box[SprintDetails] = {
    if (isRemoved)
      Failure("Cannot update removed sprint")
    else
      Full(copy(overriddenBaseStoryPointsSum = Some(base)))
  }

  def update(upd: MajorSprintDetails): Box[SprintDetails] = {
    if (isRemoved)
      Failure("Cannot update removed sprint")
    else
      (upd.name != name ||
       upd.start != start ||
       upd.end != end ||
       upd.isActive != isActive).option {
        copy(name = upd.name, start = upd.start, end = upd.end, state = SprintState(upd.isActive))
      }
  }

  def update(newDetails: SprintDetails): Box[SprintDetails] = {
    if (!state.canMoveTo(newDetails.state))
      Failure(s"Cannot change state from $state to ${newDetails.state} for sprint '$name'")
    else
      (newDetails != this).option {
        newDetails
      }
  }

  def toMajor: MajorSprintDetails = MajorSprintDetails(name, start, end, isActive)
}

case class MajorSprintDetails(name: String, start: Date, end: Date, isActive: Boolean = true) {
  def isFinished = !isActive
}

object SprintDetails {
  def apply(updateDetails: MajorSprintDetails): SprintDetails = {
    SprintDetails(updateDetails.name, updateDetails.start, updateDetails.end, updateDetails.isActive)
  }

  def apply(name: String, start: Date, end: Date, isActive: Boolean = true): SprintDetails = {
    SprintDetails(name, start, end, SprintState(isActive), overriddenBaseStoryPointsSum = None)
  }
}
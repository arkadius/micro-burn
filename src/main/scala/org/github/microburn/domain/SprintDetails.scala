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
import org.joda.time.{DateTime, Days}

import scalaz.Scalaz._

case class SprintDetails private(name: String,
                                 start: Date,
                                 end: Date,
                                 state: SprintState,
                                 overriddenBaseStoryPointsSum: Option[BigDecimal]) {

  import org.github.microburn.util.concurrent.BoxEnrichments._
  import SprintDetails._

  def isActive = state == ActiveState

  def isFinished = state == FinishedState

  def isRemoved = state == RemovedState

  def finish: Box[SprintDetails] = verifyAndMoveTo(FinishedState)

  def markRemoved: Box[SprintDetails] = verifyAndMoveTo(RemovedState)

  private def verifyAndMoveTo(newState: SprintState): Box[SprintDetails] = {
    for {
      _ <- Failure(s"Cannot change state from $state to $newState for sprint '$name'") whenNot state.canMoveTo(newState)
    } yield copy(state = newState)
  }

  def defineBaseStoryPoints(base: BigDecimal): Box[SprintDetails] = {
    for {
      _ <- validateIsActive
      _ <- Failure("Story points base must be lower than 1000") when base >= 1000
      _ <- Failure("Story points base must be defined with at most 3 numbers precision") when base.scale > 3
    } yield copy(overriddenBaseStoryPointsSum = Some(base))
  }

  def updateStartDate(start: Date): Box[SprintDetails] = {
    for {
      _ <- validateIsActive
      _ <- validateInterval(start, this.end)
    } yield copy(start = start)
  }

  def updateEndDate(end: Date): Box[SprintDetails] = {
    for {
      _ <- validateIsActive
      _ <- validateInterval(this.start, end)
    } yield copy(end = end)
  }

  def update(upd: MajorSprintDetails): Box[SprintDetails] = {
    for {
      _ <- Failure("Cannot update removed sprint") when isRemoved
      changedDetails <- (
        upd.name != this.name ||
        upd.start != this.start ||
        upd.end != this.end ||
        upd.isActive != this.isActive
      ).option {
        copy(name = upd.name, start = upd.start, end = upd.end, state = SprintState(upd.isActive))
      }
    } yield changedDetails
  }

  def update(newDetails: SprintDetails): Box[SprintDetails] = {
    for {
      _ <- Failure(s"Cannot change state from $state to ${newDetails.state} for sprint '$name'") whenNot state.canMoveTo(newDetails.state)
      changedDetails <- (
        newDetails != this
      ).option {
        newDetails
      }
    } yield changedDetails
  }

  private def validateIsActive: Box[Unit] = {
    Failure("Cannot update not active sprint") whenNot isActive
  }

  def toMajor: MajorSprintDetails = MajorSprintDetails(name, start, end, isActive)
}

case class MajorSprintDetails(name: String, start: Date, end: Date, isActive: Boolean = true) {
  def isFinished = !isActive
}

object SprintDetails {
  import org.github.microburn.util.concurrent.BoxEnrichments._

  def create(updateDetails: MajorSprintDetails): Box[SprintDetails] = {
    create(updateDetails.name, updateDetails.start, updateDetails.end, updateDetails.isActive)
  }

  def create(name: String, start: Date, end: Date, isActive: Boolean = true): Box[SprintDetails] = {
    for {
      _ <- validateInterval(start, end)
    } yield SprintDetails(name, start, end, SprintState(isActive), overriddenBaseStoryPointsSum = None)
  }

  private def validateInterval(start: Date, end: Date): Box[Unit] = {
    for {
      _ <- Failure("Start date must be before end date") whenNot start.before(end)
      _ <- Failure("Sprint cannot be longer than 1 year") when Days.daysBetween(new DateTime(start), new DateTime(end)).getDays > 365
    } yield Unit
  }



}
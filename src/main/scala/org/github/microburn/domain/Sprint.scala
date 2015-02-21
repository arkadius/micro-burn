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

import org.github.microburn.util.logging.Slf4jLogging
import org.joda.time.DateTime
import ComputationContextConversions._

case class Sprint(id: Int,
                  details: SprintDetails,
                  private val initialBoard: BoardState,
                  currentBoard: BoardState,
                  private val events: Seq[TaskEvent]) extends Slf4jLogging {

  def updateDetails(newDetails: SprintDetails)
                   (timestamp: Date): SprintUpdateResult = {
    val changedDetails = details.update(newDetails)

    val updatedSprint = copy(
      details = changedDetails openOr details
    )
    SprintUpdateResult(updatedSprint, Nil, changedDetails.isDefined, timestamp)
  }

  def update(updatedUserStories: Seq[UserStory], updateDetails: MajorSprintDetails)
            (timestamp: Date): SprintUpdateResult = {
    val updatedBoard = BoardState(updatedUserStories, timestamp)
    val newAddedEvents = currentBoard.diff(updatedBoard)
    val changedDetails = details.update(updateDetails)
    
    val updatedSprint = copy(
      details = changedDetails openOr details,
      currentBoard = updatedBoard,
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(updatedSprint, newAddedEvents, changedDetails.isDefined, timestamp)
  }
  
  def sprintHistory(implicit config: ProjectConfig): SprintHistory = measure("sprint history computation") {
    implicit val context = prepareComputationContext
    SprintHistory(
      sprintBase = sprintBase,
      columnStates = columnStatesHistory,
      sprintDetails = details
    )
  }
  
  def baseStoryPointsForStart(implicit config: ProjectConfig): BigDecimal = {
    implicit val context = prepareComputationContext
    sprintBase.baseStoryPointsForStart
  }

  private def prepareComputationContext(implicit config: ProjectConfig): ComputationContext = {
    ComputationContext(initialAfterStartPlusAcceptableDelay, initiallyDoneTaskIds)
  }

  private def initiallyDoneTaskIds(implicit config: ProjectConfig): Set[String] = {
    initialBoard.doneTasksIds
  }

  private def sprintBase(implicit context: ComputationContext): SprintBase = {
    val baseDeterminer = new SprintBaseStateDeterminer(context.config.sprintBaseDetermineMode)
    baseDeterminer.baseForSprint(
      details,
      initialAfterStartPlusAcceptableDelay,
      initialBoard.userStoriesStoryPointsSum,
      initialBoard.doneTasksStoryPointsSum
    )
  }

  def initialAfterStartPlusAcceptableDelay(implicit config: ProjectConfig): Boolean = {
    val initialDate = new DateTime(initialBoard.date)
    val startDatePlusAcceptableDelay =
      new DateTime(details.start).plusMillis(config.initialFetchAfterSprintStartAcceptableDelay.toMillis.toInt)
    initialDate.isAfter(startDatePlusAcceptableDelay)
  }
  
  private def columnStatesHistory(implicit context: ComputationContext): Seq[DateWithColumnsState] = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    val boardStatesCumulative = eventsSortedAndGrouped.scanLeft(initialBoard) { (prevBoard, currEventsGroup) =>
      currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
        boardAcc.plus(event)
      }
    }

    boardStatesCumulative.map(_.columnsState)
  }
}

case class SprintUpdateResult(updatedSprint: Sprint,
                              newAddedEvents: Seq[TaskEvent],
                              importantDetailsChange: Boolean,
                              timestamp: Date) {
  def importantBoardStateChange: Boolean = newAddedEvents.nonEmpty
}

object Sprint {
  def withEmptyEvents(id: Int, details: SprintDetails, state: BoardState): Sprint =
    Sprint(id, details, initialBoard = state, currentBoard = state, IndexedSeq.empty)
}

case class SprintHistory(sprintBase: SprintBase,
                         columnStates: Seq[DateWithColumnsState],
                         sprintDetails: SprintDetails)
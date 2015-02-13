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

case class Sprint(id: String,
                  details: SprintDetails,
                  private val initialBoard: BoardState,
                  currentBoard: BoardState,
                  private val events: Seq[TaskEvent]) {

  private def isActive = details.isActive

  def initialDate: Date = initialBoard.date

  def initialStoryPointsSum(implicit projectConfig: ProjectConfig): BigDecimal = initialBoard.userStoriesStoryPointsSum

  def initialDoneTasksStoryPointsSum(implicit projectConfig: ProjectConfig): BigDecimal = initialBoard.doneTasksStoryPointsSum

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
  
  def columnStatesHistory(implicit config: ProjectConfig): Seq[DateWithColumnsState] = {
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
  def withEmptyEvents(id: String, details: SprintDetails, state: BoardState): Sprint =
    Sprint(id, details, initialBoard = state, currentBoard = state, IndexedSeq.empty)
}
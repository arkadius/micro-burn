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
package org.github.microburn.domain.history

import org.github.microburn.domain._
import org.joda.time.DateTime
import scalaz._
import Scalaz._

class SprintHistoryComputer(details: SprintDetails, initialBoard: BoardState, events: Seq[TaskEvent]) {
  def cumulativeBoardStatesWithKnowledge(implicit config: ProjectConfig): BoardStatesWithKnowledgeCumulative = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date}
      .map { case (date, group) => group}

    val optionalSimulatedBoardStateOnStart = initialAfterStartPlusAcceptableDelay.option {
      implicit val implicitKnowledge = KnowledgeAboutLastState.assumingNoneDoneTask
      initialBoard.openNestedInSprint.copy(date = details.start)
    }

    val boardStates = optionalSimulatedBoardStateOnStart.toList ::: eventsSortedAndGrouped.scanLeft(initialBoard) { (cumulativeBoard, currEventsGroup) =>
      currEventsGroup.foldLeft(cumulativeBoard) { (accBoard, event) =>
        accBoard.plus(event)
      }
    }.toList

    val (startBoardState :: tailBoardStates) = boardStates
    val startDoneTasks = startBoardState.doneTasks(KnowledgeAboutLastState.assumingNoneDoneTask)
    val startKnowledge = SprintHistoricalKnowledge.assumingAllDoneTasksWereNotReopened(startDoneTasks)
    val startStateWithKnowledge = BoardStateWithHistoricalKnowledge(startBoardState, startKnowledge)

    val boardStatesCumulative = tailBoardStates.scanLeft(startStateWithKnowledge) {
      case (BoardStateWithHistoricalKnowledge(_, prevKnowledge), nextState) =>
        val bothOnBoardAndOutOfBoardDoneTasks = nextState.doneTasks(prevKnowledge.aboutLastState) ++ prevKnowledge.doneTasksOutOfBoard(nextState)
        val cumulativeKnowledge = prevKnowledge.withNextStateDoneTasks(bothOnBoardAndOutOfBoardDoneTasks)
        BoardStateWithHistoricalKnowledge(nextState, cumulativeKnowledge)
    }
    BoardStatesWithKnowledgeCumulative(boardStatesCumulative)
  }

  private def initialAfterStartPlusAcceptableDelay(implicit config: ProjectConfig): Boolean = {
    val initialDate = new DateTime(initialBoard.date)
    val startDatePlusAcceptableDelay =
      new DateTime(details.start).plusMillis(config.initialFetchAfterSprintStartAcceptableDelay.toMillis.toInt)
    initialDate.isAfter(startDatePlusAcceptableDelay)
  }

  case class BoardStatesWithKnowledgeCumulative(boardStatesCumulative: List[BoardStateWithHistoricalKnowledge]) {
    def sprintHistory(implicit config: ProjectConfig) = {
      val tasksOnRightFromColumns = boardStatesCumulative.map(_.tasksOnRightFromColumns)
      val initial = DateWithColumnsChanges(tasksOnRightFromColumns.head)
      val columnStates = initial :: (tasksOnRightFromColumns zip tasksOnRightFromColumns.tail).map {
        case (prevTasksOnRight, currentTasksOnRight) =>
          currentTasksOnRight.diff(prevTasksOnRight)
      }
      SprintHistory(
        sprintBase = sprintBase,
        columnStates = columnStates,
        sprintDetails = details
      )
    }

    def sprintBase(implicit config: ProjectConfig) = {
      val baseDeterminer = new SprintBaseStateDeterminer(config.sprintBaseDetermineMode)
      baseDeterminer.baseForSprint(
        details,
        boardStatesCumulative.head.userStoriesPointsSum,
        boardStatesCumulative.last.userStoriesPointsSum
      )
    }
  }

  case class BoardStateWithHistoricalKnowledge(board: BoardState, knowledge: SprintHistoricalKnowledge) {
    def userStoriesPointsSum(implicit config: ProjectConfig): BigDecimal = {
      implicit val implicitKnowledge = knowledge
      board.userStoriesPointsSum + knowledge.userStoriesPointsSumOutOfBoard(board)
    }

    def tasksOnRightFromColumns(implicit config: ProjectConfig): DateWithTasksOnRightFromColumns = {
      implicit val implicitKnowledge = knowledge
      val indexOnSum = config.nonBacklogColumns.map { boardColumn =>
        val boardColumnIndex = boardColumn.index
        val tasksOnRightOnBoard = board.tasksOnRightFromColumn(boardColumnIndex)
        val optionalOutOfBoardTasks = (boardColumn == config.lastDoneColumn).option(knowledge.doneTasksOutOfBoard(board)).toSeq.flatten
        boardColumnIndex -> (tasksOnRightOnBoard ++ optionalOutOfBoardTasks)
      }.toMap
      DateWithTasksOnRightFromColumns(board.date, indexOnSum)
    }
  }
}

case class SprintHistory(sprintBase: BigDecimal,
                         columnStates: Seq[DateWithColumnsChanges],
                         sprintDetails: SprintDetails)
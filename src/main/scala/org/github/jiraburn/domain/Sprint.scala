package org.github.jiraburn.domain

import java.util.Date
import scalaz._
import Scalaz._

case class Sprint(id: String,
                  details: SprintDetails,
                  private val initialState: SprintState,
                  currentState: SprintState,
                  private val events: Seq[TaskChanged]) {

  def isActive = details.isActive

  def initialDate: Date = initialState.date

  def initialStoryPointsSum: Int = {
    sumStoryPoints(initialState.userStories)
  }

  private def sumStoryPoints(userStories: Seq[UserStory]): Int = {
    userStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def storyPointsChanges(implicit config: ProjectConfig): Seq[DateWithColumnsState] = Sprint.storyPointsChanges(events)(initialState)

  def update(updatedUserStories: Seq[UserStory], finishSprint: Boolean)
            (timestamp: Date)
            (implicit config: ProjectConfig): SprintUpdateResult = {
    val updatedState = SprintState(updatedUserStories, timestamp)
    val currentBoard = BoardState(currentState)
    val updatedBoard = BoardState(updatedState)
    val newAddedEvents = currentBoard.diff(updatedBoard)
    val finished = isActive && finishSprint
    
    val updatedSprint = copy(
      details = if (finished) details.finish else details,
      currentState = updatedState,
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(currentState, updatedSprint, newAddedEvents, finished, timestamp)
  }
}

case class SprintState(userStories: Seq[UserStory], date: Date)

case class SprintDetails(name: String, start: Date, end: Date, isActive: Boolean) {
  def finished = !isActive

  def finish = copy(isActive = false)
}

object SprintDetails {
  def apply(name: String, start: Date, end: Date): SprintDetails = SprintDetails(name, start, end, isActive = true)
}

case class SprintUpdateResult(private val stateBeforeUpdate: SprintState, updatedSprint: Sprint, newAddedEvents: Seq[TaskChanged], sprintFinished: Boolean, timestamp: Date) {
  def importantChange(implicit config: ProjectConfig): Boolean =  importantDetailsChange || importantEventsChange

  def importantDetailsChange: Boolean = sprintFinished // co ze zmianą nazwy/dat?

  def importantEventsChange(implicit config: ProjectConfig): Boolean =
    Sprint.storyPointsChanges(newAddedEvents)(stateBeforeUpdate).exists(_.nonEmpty)
}

object Sprint {
  def withEmptyEvents(id: String, details: SprintDetails, state: SprintState): Sprint =
    Sprint(id, details, initialState = state, currentState = state, Nil)
  
  private[domain] def storyPointsChanges(events: Seq[TaskChanged])
                                        (state: SprintState)
                                        (implicit config: ProjectConfig): Seq[DateWithColumnsState] = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    lazy val boardStateStream: Stream[BoardState] =
      BoardState(state) #::
        (boardStateStream zip eventsSortedAndGrouped).map {
          case (prevBoard, currEventsGroup) =>
            currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
              boardAcc.plus(event)
            }
        }

    boardStateStream.map(DateWithColumnsState.apply).toList
  }
}

case class DateWithColumnsState(date: Date, indexOnSum: Map[Int, Int]) {
  def plus(otherIndexOnSum: Map[Int, Int]): DateWithColumnsState = {
    copy(indexOnSum = indexOnSum |+| otherIndexOnSum)
  }

  def multiply(const: Int): DateWithColumnsState = {
    copy(indexOnSum = indexOnSum.mapValues(_ * const))
  }
  
  def plus(const: Int): DateWithColumnsState = {
    copy(indexOnSum = indexOnSum.mapValues(_ + const))
  }

  def storyPointsForColumn(boardColumnIndex: Int) = indexOnSum.getOrElse(boardColumnIndex, 0)

  def nonEmpty = indexOnSum.values.exists(_ != 0)
}

object DateWithColumnsState {
  def zero(implicit config: ProjectConfig): DateWithColumnsState = zero(new Date(0))

  def zero(date: Date)(implicit config: ProjectConfig): DateWithColumnsState = DateWithColumnsState.const(0)(date)

  def const(c: Int)(date: Date)(implicit config: ProjectConfig) = DateWithColumnsState(date, config.boardColumns.map(_.index -> c).toMap)

  def apply(sprintState: SprintState)(implicit config: ProjectConfig): DateWithColumnsState = {
    DateWithColumnsState(BoardState(sprintState))
  }

  def apply(boardState: BoardState)(implicit config: ProjectConfig): DateWithColumnsState = {
    val indexOnSum = config.boardColumns.map(_.index).map { boardColumnIndex =>
      boardColumnIndex -> boardState.storyPointsOnRightFromBoardColumn(boardColumnIndex)
    }.toMap
    DateWithColumnsState(boardState.date, indexOnSum)
  }

}
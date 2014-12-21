package org.github.jiraburn.domain

import java.util.Date
import scalaz._
import Scalaz._

case class Sprint(id: String,
                  details: SprintDetails,
                  private val initialUserStories: Seq[UserStory],
                  currentUserStories: Seq[UserStory],
                  private val events: Seq[TaskChanged]) {

  def isActive = details.isActive

  def initialStoryPoints: Int = {
    sumStoryPoints(initialUserStories)
  }

  private def sumStoryPoints(userStories: Seq[UserStory]): Int = {
    userStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def storyPointsChanges(implicit config: ProjectConfig): Seq[DateWithStoryPoints] = Sprint.storyPointsChanges(events)(initialUserStories)

  def update(updatedUserStories: Seq[UserStory], finishSprint: Boolean)
            (timestamp: Date)
            (implicit config: ProjectConfig): SprintUpdateResult = {
    val currentBoard = BoardState(currentUserStories, new Date(0))
    val updatedBoard = BoardState(updatedUserStories, timestamp)
    val newAddedEvents = currentBoard.diff(updatedBoard)
    val finished = isActive && finishSprint
    
    val updatedSprint = copy(
      details = if (finished) details.finish else details,
      currentUserStories = updatedUserStories,    
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(currentUserStories, updatedSprint, newAddedEvents, finished, timestamp)
  }
}

case class SprintDetails(name: String, start: Date, end: Date, isActive: Boolean) {
  def finished = !isActive

  def finish = copy(isActive = false)
}

object SprintDetails {
  def apply(name: String, start: Date, end: Date): SprintDetails = SprintDetails(name, start, end, isActive = true)
}

case class SprintUpdateResult(private val userStoriesBeforeUpdate: Seq[UserStory], updatedSprint: Sprint, newAddedEvents: Seq[TaskChanged], sprintFinished: Boolean, timestamp: Date) {
  def importantChange(implicit config: ProjectConfig): Boolean =  importantDetailsChange || importantEventsChange

  def importantDetailsChange: Boolean = sprintFinished // co ze zmianą nazwy/dat?

  def importantEventsChange(implicit config: ProjectConfig): Boolean =
    Sprint.storyPointsChanges(newAddedEvents)(userStoriesBeforeUpdate).exists(_.nonEmpty)
}

object Sprint {
  def withEmptyEvents(id: String, details: SprintDetails, userStories: Seq[UserStory]): Sprint =
    Sprint(id, details, initialUserStories = userStories, currentUserStories = userStories, Nil)
  
  private[domain] def storyPointsChanges(events: Seq[TaskChanged])
                                        (userStories: Seq[UserStory])
                                        (implicit config: ProjectConfig): Seq[DateWithStoryPoints] = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    lazy val boardStateStream: Stream[BoardState] =
      BoardState(userStories, new Date(0)) #::
        (boardStateStream zip eventsSortedAndGrouped).map {
          case (prevBoard, currEventsGroup) =>
            currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
              boardAcc.plus(event)
            }
        }

    lazy val storyPointsDiffStream: Stream[DateWithStoryPoints] =
      (boardStateStream zip boardStateStream.tail).map {
        case (prev, next) =>
          val indexOnSum = config.boardColumns.map(_.index).map { boardColumnIndex =>
            val diff = prev.taskAtRighttFromBoardColumn(boardColumnIndex) - next.taskAtRighttFromBoardColumn(boardColumnIndex)
            boardColumnIndex -> diff
          }.toMap
          DateWithStoryPoints(next.date, indexOnSum)
      }

    lazy val storyPointsChangeStream: Stream[DateWithStoryPoints] =
      DateWithStoryPoints.zero #::
        (storyPointsChangeStream zip storyPointsDiffStream).map {
          case (acc, diff) =>
            acc.plus(diff)
        }

    storyPointsChangeStream.drop(1).take(eventsSortedAndGrouped.size)
  }
}

case class DateWithStoryPoints(date: Date, indexOnSum: Map[Int, Int]) {
  def plus(other: DateWithStoryPoints): DateWithStoryPoints = {
    other.copy(indexOnSum = this.indexOnSum |+| other.indexOnSum)
  }

  def plus(const: Int): DateWithStoryPoints = {
    copy(indexOnSum = indexOnSum.mapValues(_ + const))
  }

  def storyPointsForColumn(boardColumnIndex: Int) = indexOnSum.getOrElse(boardColumnIndex, 0)

  def nonEmpty = indexOnSum.values.exists(_ != 0)
}

object DateWithStoryPoints {
  def zero(implicit config: ProjectConfig): DateWithStoryPoints = zero(new Date(0))

  def zero(date: Date)(implicit config: ProjectConfig): DateWithStoryPoints =
    DateWithStoryPoints(date, config.boardColumns.map(_.index -> 0).toMap)

}
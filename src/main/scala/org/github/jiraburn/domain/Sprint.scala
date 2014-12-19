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

  def storyPointsChanges(implicit config: ProjectConfig): Seq[DateWithStoryPoints] = Sprint.storyPointsChanges(events)(this)

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
    SprintUpdateResult(updatedSprint, newAddedEvents, finished, timestamp)
  }
}

case class SprintDetails(name: String, from: Date, to: Date, isActive: Boolean) {
  def finish = copy(isActive = false)
}

object SprintDetails {
  def apply(name: String, from: Date, to: Date): SprintDetails = SprintDetails(name, from, to, isActive = true)
}

case class SprintUpdateResult(updatedSprint: Sprint, newAddedEvents: Seq[TaskChanged], sprintFinished: Boolean, timestamp: Date) {
  def importantChange(implicit config: ProjectConfig): Boolean =  importantDetailsChange || importantEventsChange

  def importantDetailsChange: Boolean = sprintFinished // co ze zmianą nazwy/dat?

  def importantEventsChange(implicit config: ProjectConfig): Boolean =
    Sprint.storyPointsChanges(newAddedEvents)(updatedSprint).exists(_.storyPoints > 0)
}

object Sprint {
  def withEmptyEvents(id: String, details: SprintDetails, userStories: Seq[UserStory]): Sprint =
    Sprint(id, details, initialUserStories = userStories, currentUserStories = userStories, Nil)
  
  private[domain] def storyPointsChanges(events: Seq[TaskChanged])
                                        (sprint: Sprint)
                                        (implicit config: ProjectConfig): Seq[DateWithStoryPoints] = {
    val initialAndCurrentUserStoriesIds = (sprint.initialUserStories ++ sprint.currentUserStories).map(_.taskId).toSet

    val initialAndCurrentEventsSortedAndGrouped = events
      .filter { event => initialAndCurrentUserStoriesIds.contains(event.parentTaskId) }
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    lazy val boardStateStream: Stream[BoardState] =
      BoardState(sprint.initialUserStories, new Date(0)) #::
        (boardStateStream zip initialAndCurrentEventsSortedAndGrouped).map {
          case (prevBoard, currEventsGroup) =>
            currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
              boardAcc.plus(event)
            }
        }

    lazy val storyPointsDiffStream: Stream[DateWithStoryPoints] =
      (boardStateStream zip boardStateStream.tail).map {
        case (prev, next) =>
          DateWithStoryPoints(next.date, prev.closedCount - next.closedCount)
      }

    lazy val storyPointsChangeStream: Stream[DateWithStoryPoints] =
      DateWithStoryPoints.zero #::
        (storyPointsChangeStream zip storyPointsDiffStream).map {
          case (acc, diff) =>
            acc.plus(diff)
        }

    storyPointsChangeStream.drop(1).take(initialAndCurrentEventsSortedAndGrouped.size)
  }
}

case class DateWithStoryPoints(date: Date, storyPoints: Int) {
  def plus(other: DateWithStoryPoints): DateWithStoryPoints = {
    other.copy(storyPoints = this.storyPoints + other.storyPoints)
  }
}

object DateWithStoryPoints {
  def zero: DateWithStoryPoints = DateWithStoryPoints(new Date(0L), 0)
}
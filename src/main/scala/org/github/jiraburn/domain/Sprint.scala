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
    implicit val timestampImplicit = timestamp
    val currentTasksById = taskById(currentUserStories)
    val updatedTasksById = taskById(updatedUserStories)
    val allTaskIds = currentTasksById.keySet ++ updatedTasksById.keySet
    val newAddedEvents = for {
      taskId <- allTaskIds.toSeq
      optionalCurrentTask = currentTasksById.get(taskId)
      optionalUpdatedTask = updatedTasksById.get(taskId)
      event <- delta(optionalCurrentTask, optionalUpdatedTask)
    } yield event
    val finished = isActive && finishSprint
    
    val updatedSprint = copy(
      details = if (finished) details.finish else details,
      currentUserStories = updatedUserStories,    
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(updatedSprint, newAddedEvents, finished, timestamp)
  }

  private def delta(state: Option[Task], nextState: Option[Task])
                   (implicit timestamp: Date): Option[TaskChanged] = {
    Seq(state, nextState).flatten match {
      case Nil => None
      case one :: Nil => Some(prepareDelta(state, nextState))
      case definedState :: definedNextState :: Nil =>
        val statusChanged = definedNextState.status != definedState.status
        val storyPointsWithoutSubTasksChanged =
          definedNextState.storyPointsWithoutSubTasks != definedState.storyPointsWithoutSubTasks
        // co ze zmianą parenta/typu zadania?
        (statusChanged || storyPointsWithoutSubTasksChanged).option(prepareDelta(state, nextState))
    }
  }

  private def prepareDelta(state: Option[Task], nextState: Option[Task])
                          (implicit timestamp: Date): TaskChanged = {
    val task = (nextState orElse state).get
    TaskChanged(
      task.taskId, task.parentUserStoryId, task.isTechnicalTask,
      state.map(TaskState.apply), nextState.map(TaskState.apply),
      timestamp
    )
  }

  private def taskById(userStories: Seq[UserStory]): Map[String, Task] = {
    val flattenTasks = for {
      userStory <- userStories
      task <- userStory.technicalTasks :+ userStory
    } yield task
    flattenTasks.groupBy(_.taskId).mapValues(_.head)
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

    lazy val storyPointsStream: Stream[DateWithStoryPoints] =
      DateWithStoryPoints.zero #::
        storyPointsStream.zip(initialAndCurrentEventsSortedAndGrouped).map {
          case (prevSum, currEventsGroup) =>
            currEventsGroup.foldLeft(prevSum) { (sum, event) =>
              sum.accumulateWithEvent(event)
            }
        }

    storyPointsStream.drop(1).take(initialAndCurrentEventsSortedAndGrouped.size)
  }
}
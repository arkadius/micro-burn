package com.example.domain

import java.util.Date

case class Sprint(id: String, initialUserStories: Seq[UserStory], currentUserStories: Seq[UserStory], events: Seq[TaskEvent]) {
  def summedInitialStoryPoints: Int = {
    initialUserStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def storyPointsChanges: Seq[DateWithStoryPoints] = Sprint.storyPointsChanges(events)(this)

  def userStoriesUpdated(updatedUserStories: Seq[UserStory])(timestamp: Date): UserStoriesUpdateResult = {
    val initialUserStoriesIds = initialUserStories.map(_.taskId).toSet
    val currentTasksById = Sprint.flattenTasks(currentUserStories).groupBy(_.taskId).mapValues(_.head)
    // interesują nas tylko zdarzenia dla nowych zadań, stare które zostały usunięte ze sprintu olewamy
    // nowe, które nie były dostępne ostatnio też olewamy, bo interesują nast tylko zmiany a nie stan bieżący
    val newAddedEvents = for {
      updatedTask <- Sprint.flattenTasks(updatedUserStories)
      currentTask <- currentTasksById.get(updatedTask.taskId)
      parentUserStoryFromInitialScope = initialUserStoriesIds.contains(updatedTask.parentUserStoryId)
      event <- eventsForTaskUpdate(currentTask, updatedTask, parentUserStoryFromInitialScope)(timestamp)
    } yield event
    
    val updatedSprint = copy(currentUserStories = updatedUserStories, events = events ++ newAddedEvents)
    UserStoriesUpdateResult(updatedSprint, newAddedEvents)
  }

  private def eventsForTaskUpdate(currentTask: Task, updatedTask: Task, parentUserStoryFromInitialScope: Boolean)
                                 (implicit timestamp: Date): Option[TaskEvent] = {
    if (currentTask.isOpened && updatedTask.isCompleted)
      currentTask.finish(parentUserStoryFromInitialScope)
    else if (currentTask.isCompleted && updatedTask.isOpened)
      currentTask.reopen(parentUserStoryFromInitialScope)
    else
      None
  }
}

case class UserStoriesUpdateResult(updatedSprint: Sprint, newAddedEvents: Seq[TaskEvent]) {
  def importantChange: Boolean = Sprint.storyPointsChanges(newAddedEvents)(updatedSprint).exists(_.storyPoints > 0)
}

object Sprint {
  def withEmptyEvents(id: String, userStories: Seq[UserStory]): Sprint =
    new Sprint(id, initialUserStories = userStories, currentUserStories = userStories, Nil)
  
  private[domain] def storyPointsChanges(events: Seq[TaskEvent])(sprint: Sprint): Seq[DateWithStoryPoints] = {
    val initialAndCurrentUserStoriesIds = (sprint.initialUserStories ++ sprint.currentUserStories).map(_.taskId).toSet

    // odfiltrujemy tylko inicjalnie dodane zadania
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

  private def flattenTasks(userStories: Seq[UserStory]): Seq[Task] =
    for {
      userStory <- userStories
      task <- userStory.technicalTasks :+ userStory
    } yield task  
}
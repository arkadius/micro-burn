package com.example.domain

import java.util.Date

case class Sprint(initialStories: Seq[UserStory], currentUserStories: Seq[UserStory], events: Seq[TaskEvent]) {
  def summedInitialStoryPoints: Int = {
    initialStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def storyPointsChanges: Seq[DateWithStoryPoints] = {
    val initialAndCurrentTaskIds = tasksById(initialStories ++ currentUserStories).keySet

    val initialAndCurrentEventsSortedAndGrouped = events
      .filter { event => initialAndCurrentTaskIds.contains(event.taskId) }
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

  def userStoriesUpdated(userStoriesUpdate: Seq[UserStory])(timestamp: Date): Sprint = {
    val initialUserStoriesIds = initialStories.map(_.taskId).toSet
    val currentTasksById = tasksById(currentUserStories)
    // interesują nas tylko zdarzenia dla nowych zadań, stare które zostały usunięte ze sprintu olewamy
    // nowe, które nie były dostępne ostatnio też olewamy, bo interesują nast tylko zmiany a nie stan bieżący
    val eventsAfterUpdate = for {
      userStory <- userStoriesUpdate
      taskUpdate <- userStory.flattenTasks
      currentTask <- currentTasksById.get(taskUpdate.taskId)
      parentUserStoryFromInitialScope = initialUserStoriesIds.contains(userStory.taskId)
      event <- eventsForTaskUpdate(currentTask, taskUpdate, parentUserStoryFromInitialScope)(timestamp)
    } yield  event
    copy(currentUserStories = userStoriesUpdate, events = events ++ eventsAfterUpdate)
  }

  private def tasksById(userStories: Seq[UserStory]): Map[String, Task] = {
    val tasks = for {
      userStory <- userStories
      task <- userStory.flattenTasks
    } yield task
    tasks.groupBy(_.taskId).mapValues(_.head)
  }

  private def eventsForTaskUpdate(currentTask: Task, taskUpdate: Task, parentUserStoryFromInitialScope: Boolean)
                                 (implicit timestamp: Date): Option[TaskEvent] = {
    if (currentTask.isOpened && taskUpdate.isCompleted)
      currentTask.finish(parentUserStoryFromInitialScope)
    else if (currentTask.isCompleted && taskUpdate.isOpened)
      currentTask.reopen(parentUserStoryFromInitialScope)
    else
      None
  }
}

object Sprint {
  def withEmptyEvents(userStories: Seq[UserStory]): Sprint =
    new Sprint(initialStories = userStories, currentUserStories = userStories, Nil)
}
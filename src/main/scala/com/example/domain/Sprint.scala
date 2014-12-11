package com.example.domain

import java.util.Date

class Sprint(userStories: Seq[UserStory], events: Seq[TaskEvent]) {

  def summedStoryPoints: Int = {
    userStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def storyPointsChanges: Seq[DateWithStoryPoints] = {
    val currentTaskIds = flattenTasks(userStories).map(_.taskId).toSet
    val currentEventsSortedAndGrouped = events
      .filter { event => currentTaskIds.contains(event.taskId) }
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    lazy val storyPointsStream: Stream[DateWithStoryPoints] =
      DateWithStoryPoints.zero #::
      storyPointsStream.zip(currentEventsSortedAndGrouped).map {
        case (prevSum, currEventsGroup) =>
          currEventsGroup.foldLeft(prevSum) { (sum, event) =>
            sum.accumulateWithEvent(event)
          }
      }

    storyPointsStream.drop(1).take(currentEventsSortedAndGrouped.size).toSeq
  }

  def userStoriesUpdated(userStoriesUpdate: Seq[UserStory])(timestamp: Date): Sprint = {
    val currentTasksById = flattenTasks(userStories).groupBy(_.taskId).mapValues(_.head)
    // interesują nas tylko zdarzenia dla nowych zadań, stare które zostały usunięte ze sprintu olewamy
    val eventsAfterUpdate = for {
      taskUpdate <- flattenTasks(userStoriesUpdate)
      currentTask <- currentTasksById.get(taskUpdate.taskId)
      event <- eventsForTaskUpdate(currentTask, taskUpdate)(timestamp)
    } yield  event
    // nie czyścimy zdarzeń z tych dotyczących nieistniejących już tasków, bo jest możliwość, że wrócą -
    // odfiltujemy je przy wychiąganiu zmian
    new Sprint(userStoriesUpdate, events ++ eventsAfterUpdate)
  }

  private def flattenTasks(userStories: Seq[UserStory]): Seq[Task] = {
    for {
      userStory <- userStories
      task <- userStory.technicalTasks :+ userStory
    } yield task
  }

  private def eventsForTaskUpdate(currentTask: Task, taskUpdate: Task)
                                 (implicit timestamp: Date): Option[TaskEvent] = {
    if (currentTask.isOpened && taskUpdate.isCompleted)
      currentTask.finish
    else if (currentTask.isCompleted && taskUpdate.isOpened)
      currentTask.reopen
    else
      None
  }

}

object Sprint {
  def withEmptyEvents(userStories: Seq[UserStory]): Sprint = new Sprint(userStories, Nil)
}
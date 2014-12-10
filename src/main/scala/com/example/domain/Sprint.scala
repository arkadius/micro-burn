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
    val currentEventsSorted = events.filter { event => currentTaskIds.contains(event.taskId) }.sortBy(_.date)
    lazy val storyPointsStream: Stream[DateWithStoryPoints] =
      currentEventsSorted.head.toDateWithStoryPoints #::
      storyPointsStream.tail.zip(currentEventsSorted).map {
        case (prevSum, currEvent) =>
          prevSum.accumulateWithEvent(currEvent)
      }
    storyPointsStream.toSeq
  }

  def userStoriesUpdated(userStoriesUpdate: Seq[UserStory])
                        (implicit timestamp: Date): Sprint = {
    val currentTasksById = flattenTasks(userStories).groupBy(_.taskId).mapValues(_.head)
    // interesują nas tylko zdarzenia dla nowych zadań, stare które zostały usunięte ze sprintu olewamy
    val eventsAfterUpdate = for {
      taskUpdate <- flattenTasks(userStoriesUpdate)
      event <- eventsForTaskUpdate(currentTasksById.get(taskUpdate.taskId), taskUpdate)
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

  private def eventsForTaskUpdate(optionalCurrentTask: Option[Task], taskUpdate: Task)
                                 (implicit timestamp: Date): Option[TaskEvent] = optionalCurrentTask match {
    case Some(currentTask) if currentTask.isOpened && taskUpdate.isCompleted => Some(currentTask.finish)
    case Some(currentTask) if currentTask.isCompleted && taskUpdate.isOpened => Some(currentTask.reopen)
    // jeśli nowe zadanie jest od razu zakończone to zaliczamy mu punkty
    case None if taskUpdate.isCompleted => taskUpdate.optionalStoryPoints.map(TaskCompleted(taskUpdate.taskId, timestamp, _))
    // jeśli nowe zadanie natomiast jest otwarta to nie traktujemy go jako ponownie otwarte
    // (bo jest mała szansa że było zamknięte, znikło z tablicy i wróciło jako otwarte, zazwyczaj jest dobierane)
    case _ => None
  }

}

object Sprint {
  def withEmptyEvents(userStories: Seq[UserStory]): Sprint = new Sprint(userStories, Nil)
}
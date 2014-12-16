package org.github.jiraburn.domain

import java.util.Date

sealed trait TaskEvent {
  def taskId: String
  def parentTaskId: String
  def date: Date
  def storyPoints: Int
  def taskFromInitialScope: Boolean

  def toDateWithStoryPoints = DateWithStoryPoints(date, storyPoints)
}

case class TaskReopened(taskId: String, parentTaskId: String, taskFromInitialScope: Boolean, date: Date, storyPoints: Int) extends TaskEvent

case class TaskCompleted(taskId: String, parentTaskId: String, taskFromInitialScope: Boolean, date: Date, storyPoints: Int) extends TaskEvent

case class DateWithStoryPoints(date: Date, storyPoints: Int) {
  def accumulateWithEvent(event: TaskEvent) = {
    val maxDate = if (event.date.after(date)) {
      event.date
    } else {
      date
    }
    val accumulatedStoryPoints = event match {
      case TaskCompleted(_, _, _, _, esp) => storyPoints - esp
      case TaskReopened(_, _, _, _, esp)  => storyPoints + esp
    }
    DateWithStoryPoints(maxDate, accumulatedStoryPoints)
  }
}

object DateWithStoryPoints {
  def zero: DateWithStoryPoints = DateWithStoryPoints(new Date(0L), 0)
}
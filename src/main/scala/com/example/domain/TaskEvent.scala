package com.example.domain

import java.util.Date

sealed trait TaskEvent {
  def taskId: String
  def date: Date
  def storyPoints: Int

  def toDateWithStoryPoints = DateWithStoryPoints(date, storyPoints)
}

case class TaskReopened(taskId: String, date: Date, storyPoints: Int) extends TaskEvent

case class TaskCompleted(taskId: String, date: Date, storyPoints: Int) extends TaskEvent

case class DateWithStoryPoints(date: Date, storyPoints: Int) {
  def accumulateWithEvent(event: TaskEvent) = {
    val maxDate = if (event.date.after(date)) {
      event.date
    } else {
      date
    }
    val accumulatedStoryPoints = event match {
      case TaskCompleted(_, _, esp) => storyPoints - esp
      case TaskReopened(_, _, esp)    => storyPoints + esp
    }
    DateWithStoryPoints(maxDate, accumulatedStoryPoints)
  }
}
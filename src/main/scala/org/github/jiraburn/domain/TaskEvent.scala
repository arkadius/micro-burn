package org.github.jiraburn.domain

import java.util.Date

sealed trait TaskEvent {
  def taskId: String
  def parentUserStoryId: String
  def isTechnicalTask: Boolean
  def date: Date
}

case class TaskAdded(taskId: String,
                     parentUserStoryId: String,
                     isTechnicalTask: Boolean,
                     taskName: String,
                     optionalStoryPoints: Option[Int],
                     status: Int,
                     date: Date) extends TaskEvent

case class TaskRemoved(taskId: String,
                       parentUserStoryId: String,
                       isTechnicalTask: Boolean,
                       date: Date) extends TaskEvent

case class TaskUpdated(taskId: String,
                       parentUserStoryId: String,
                       isTechnicalTask: Boolean,
                       taskName: String,
                       optionalStoryPoints: Option[Int],
                       status: Int,
                       date: Date) extends TaskEvent

object TaskAdded {
  def apply(task: Task)(implicit timestamp: Date): TaskAdded = TaskAdded(
    taskId = task.taskId,
    parentUserStoryId = task.parentUserStoryId,
    isTechnicalTask = task.isTechnicalTask,
    taskName = task.taskName,
    optionalStoryPoints = task.optionalStoryPoints,
    status = task.status,
    date = timestamp)
}

object TaskRemoved {
  def apply(task: Task)(implicit timestamp: Date): TaskRemoved = TaskRemoved(
    taskId = task.taskId,
    parentUserStoryId = task.parentUserStoryId,
    isTechnicalTask = task.isTechnicalTask,
    date = timestamp)
}

object TaskUpdated {
  def apply(task: Task)(implicit timestamp: Date): TaskUpdated = TaskUpdated(
    taskId = task.taskId,
    parentUserStoryId = task.parentUserStoryId,
    isTechnicalTask = task.isTechnicalTask,
    taskName = task.taskName,
    optionalStoryPoints = task.optionalStoryPoints,
    status = task.status,
    date = timestamp)
}
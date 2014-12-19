package org.github.jiraburn.domain

import java.util.Date

case class TaskChanged(taskId: String,
                       parentTaskId: String,
                       isTechnicalTask: Boolean,
                       optionalFromState: Option[TaskState],
                       optionalToState: Option[TaskState],
                       date: Date) {

  def taskWasAdded = optionalFromState.isEmpty && optionalToState.isDefined

  def taskWasRemoved = optionalFromState.isDefined && optionalToState.isEmpty
}
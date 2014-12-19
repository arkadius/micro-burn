package org.github.jiraburn.domain

import java.util.Date

case class TaskChanged(taskId: String,
                       parentTaskId: String,
                       isTechnicalTask: Boolean,
                       optionalToState: Option[TaskState],
                       date: Date)
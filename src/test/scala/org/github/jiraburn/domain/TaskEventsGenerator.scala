package org.github.jiraburn.domain

import java.util.Date

object TaskEventsGenerator {

  def completedEvent(implicit config: ProjectConfig) = TaskChanged(
    "fooTaskId",
    "fooParentTaskId",
    isTechnicalTask = false,
    Some(TaskState(config.firstNotClosingStatus, 123)),
    Some(TaskState(config.firstClosingStatus, 123)),
    new Date
  )

  def reopenedEvent(implicit config: ProjectConfig) = TaskChanged(
    "fooTaskId",
    "fooParentTaskId",
    isTechnicalTask = false,
    Some(TaskState(config.firstClosingStatus, 123)),
    Some(TaskState(config.firstNotClosingStatus, 123)),
    new Date
  )

}
package org.github.jiraburn.domain

import java.util.Date

object TaskEventsGenerator {

  def completedEvent(implicit config: ProjectConfig) = TaskChanged(
    "fooTaskId",
    "fooParentTaskId",
    isTechnicalTask = false,
    Some(TaskState(ProjectConfigUtils.firstClosingStatus, 123)),
    new Date
  )

  def reopenedEvent(implicit config: ProjectConfig) = TaskChanged(
    "fooTaskId",
    "fooParentTaskId",
    isTechnicalTask = false,
    Some(TaskState(ProjectConfigUtils.firstNotClosingStatus, 123)),
    new Date
  )

}
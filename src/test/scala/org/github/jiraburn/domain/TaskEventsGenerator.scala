package org.github.jiraburn.domain

import java.util.Date

object TaskEventsGenerator {

  def completedEvent(implicit config: ProjectConfig) = TaskUpdated(
    taskId = "fooTaskId",
    parentUserStoryId = "fooParentTaskId",
    isTechnicalTask = false,
    taskName = "fooTaskName",
    optionalStoryPoints = Some(123),
    status = ProjectConfigUtils.firstClosingStatus,
    date = new Date
  )

  def reopenedEvent(implicit config: ProjectConfig) = TaskUpdated(
    taskId = "fooTaskId",
    parentUserStoryId = "fooParentTaskId",
    isTechnicalTask = false,
    taskName = "fooTaskName",
    optionalStoryPoints = Some(123),
    status = ProjectConfigUtils.firstNotClosingStatus,
    date = new Date
  )

}
package org.github.jiraburn.domain

import java.util.Date

object TaskEventsGenerator {

  def completedEvent(implicit config: ProjectConfig) = TaskChanged(
    "fooTaskId",
    "fooParentTaskId",
    config.firstNotClosingStatus,
    config.firstClosingStatus,
    123,
    123,
    new Date
  )

  def reopenedEvent(implicit config: ProjectConfig) = TaskChanged(
    "fooTaskId",
    "fooParentTaskId",
    config.firstClosingStatus,
    config.firstNotClosingStatus,
    123,
    123,
    new Date
  )

}
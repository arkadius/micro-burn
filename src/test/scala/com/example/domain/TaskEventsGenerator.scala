package com.example.domain

import java.util.Date

object TaskEventsGenerator {

  def completedEvent = TaskCompleted("fooTaskId", "fooParentTaskId", true, new Date, 123)

  def reopenedEvent = TaskReopened("fooTaskId", "fooParentTaskId", true, new Date, 123)

}

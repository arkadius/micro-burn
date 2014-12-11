package com.example.repository

import com.example.domain.TaskEvent

trait TaskEventsRepository {
  def appendTaskEvents(events: TaskEvent): Unit

  def loadTaskEvents: Seq[TaskEvent]
}
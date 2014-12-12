package com.example.repository

import java.util.Date

import com.example.domain.{Sprint, TaskEvent, UserStoriesUpdateResult}
import com.typesafe.config.{ConfigFactory, Config}

class SprintRepository(config: Config) {

  private var cachedEvents: Seq[TaskEvent] = IndexedSeq()

  private val storiesRepo = SprintScopeRepository(config)
  private val eventsRepo = TaskEventsRepository(config)
  
  def loadSprint(sprintId: String): Option[Sprint] =
    for {
      initial <- storiesRepo.loadInitialUserStories(sprintId)
      current <- storiesRepo.loadCurrentUserStories(sprintId)
      events = eventsRepo.loadTaskEvents    
    } yield Sprint(sprintId, initial, current, events) 

  def saveUpdateResult(updateResult: UserStoriesUpdateResult): Unit = {
    saveCurrentUserStories(updateResult)
    appendTasksEventsIfNecessary(updateResult)
  }

  private def saveCurrentUserStories(updateResult: UserStoriesUpdateResult): Unit = {
    storiesRepo.saveCurrentUserStories(updateResult.updatedSprint)(updateResult.timestamp)
    storiesRepo.cleanUnnecessaryStates(updateResult.updatedSprint.id)
  }

  def appendTasksEventsIfNecessary(updateResult: UserStoriesUpdateResult): Unit = {
    cachedEvents ++= updateResult.newAddedEvents
    if (updateResult.importantChange)
      flushTaskEvents()
  }

  def flushTaskEvents(): Unit = {
    eventsRepo.appendTasksEvents(cachedEvents)
    cachedEvents = IndexedSeq()
  }

}
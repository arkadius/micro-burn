package com.example.repository

import java.io.File

import com.example.domain.{Sprint, TaskEvent, UserStoriesUpdateResult}

case class SprintRepository private(private val sprintRoot: File,
                                    private val sprintId: String,
                                    private val cachedEvents: Seq[TaskEvent]) {
  private val detailsRepo: SprintDetailsRepository = SprintDetailsRepository(sprintRoot)
  private val storiesRepo = SprintScopeRepository(sprintRoot)
  private val eventsRepo = TaskEventsRepository(sprintRoot)
  
  def loadSprint: Option[Sprint] =
    for {
      details <- detailsRepo.loadDetails
      initial <- storiesRepo.loadInitialUserStories
      current <- storiesRepo.loadCurrentUserStories
      events = eventsRepo.loadTaskEvents    
    } yield Sprint(sprintId, details, initial, current, events)

  def saveUpdateResult(updateResult: UserStoriesUpdateResult): SprintRepository = {
    require(updateResult.updatedSprint.id == sprintId)
    saveCurrentUserStories(updateResult)
    appendTasksEventsIfNecessary(updateResult)
  }

  private def saveCurrentUserStories(updateResult: UserStoriesUpdateResult): SprintRepository = {
    storiesRepo.saveCurrentUserStories(updateResult.updatedSprint)(updateResult.timestamp)
    storiesRepo.cleanUnnecessaryStates()
    this
  }

  private def appendTasksEventsIfNecessary(updateResult: UserStoriesUpdateResult): SprintRepository = {
    val updatedRepo = copy(cachedEvents = cachedEvents ++ updateResult.newAddedEvents)
    if (updateResult.importantChange)
      updatedRepo.flushTaskEvents()
    else
      updatedRepo
  }

  def flushTaskEvents(): SprintRepository = {
    eventsRepo.appendTasksEvents(cachedEvents)
    copy(cachedEvents = IndexedSeq())
  }
}

object SprintRepository {
  def apply(sprintRoot: File, sprintId: String) = new SprintRepository(sprintRoot, sprintId, IndexedSeq())
}
package org.github.jiraburn.repository

import java.io.File
import java.util.Date

import org.github.jiraburn.domain.{ProjectConfig, Sprint, SprintUpdateResult, TaskChanged}

case class SprintRepository private(private val sprintRoot: File,
                                    private val sprintId: String,
                                    private val cachedEvents: Seq[TaskChanged]) {
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

  def saveSprint(sprint: Sprint): SprintRepository = {
    require(sprint.id == sprintId)
    detailsRepo.saveDetails(sprint)
    storiesRepo.saveCurrentUserStories(sprint)
    this
  }

  def saveUpdateResult(updateResult: SprintUpdateResult)
                      (implicit config: ProjectConfig): SprintRepository = {
    require(updateResult.updatedSprint.id == sprintId)
    saveDetailsIfNecessary(updateResult)
      .saveCurrentUserStories(updateResult)
      .appendTasksEventsIfNecessary(updateResult)
  }

  private def saveDetailsIfNecessary(updateResult: SprintUpdateResult): SprintRepository = {
    if (updateResult.importantDetailsChange)
      detailsRepo.saveDetails(updateResult.updatedSprint)
    this
  }

  private def saveCurrentUserStories(updateResult: SprintUpdateResult): SprintRepository = {
    storiesRepo.saveCurrentUserStories(updateResult.updatedSprint)
    storiesRepo.cleanUnnecessaryStates()
    this
  }

  private def appendTasksEventsIfNecessary(updateResult: SprintUpdateResult)
                                          (implicit config: ProjectConfig): SprintRepository = {
    val updatedRepo = copy(cachedEvents = cachedEvents ++ updateResult.newAddedEvents)
    if (updateResult.importantEventsChange)
      updatedRepo.flushTaskEvents()
    else
      updatedRepo
  }

  def flush(): SprintRepository = {
    flushTaskEvents()
  }

  private def flushTaskEvents(): SprintRepository = {
    eventsRepo.appendTasksEvents(cachedEvents)
    copy(cachedEvents = IndexedSeq())
  }
}

object SprintRepository {
  def apply(sprintRoot: File, sprintId: String) = new SprintRepository(sprintRoot, sprintId, IndexedSeq())
}
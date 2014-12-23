package org.github.jiraburn.repository

import java.io.File

import org.github.jiraburn.domain.{Sprint, SprintUpdateResult}

class SprintRepository(sprintRoot: File, sprintId: String) {
  private val detailsRepo: SprintDetailsRepository = SprintDetailsRepository(sprintRoot)
  private val storiesRepo = BoardStateRepository(sprintRoot)
  private val eventsRepo = TaskEventsRepository(sprintRoot)
  
  def loadSprint: Option[Sprint] =
    for {
      details <- detailsRepo.loadDetails
      initial <- storiesRepo.loadInitialUserStories
      current <- storiesRepo.loadCurrentUserStories
      events = eventsRepo.loadTaskEvents    
    } yield Sprint(sprintId, details, initial, current, events)

  def saveSprint(sprint: Sprint): Unit = {
    require(sprint.id == sprintId)
    detailsRepo.saveDetails(sprint)
    storiesRepo.saveCurrentUserStories(sprint)
  }

  def saveUpdateResult(updateResult: SprintUpdateResult): Unit = {
    require(updateResult.updatedSprint.id == sprintId)
    saveDetailsIfNecessary(updateResult)
    saveCurrentUserStories(updateResult)
    appendTasksEventsIfNecessary(updateResult)
  }

  private def saveDetailsIfNecessary(updateResult: SprintUpdateResult): Unit = {
    if (updateResult.importantDetailsChange)
      detailsRepo.saveDetails(updateResult.updatedSprint)
  }

  private def saveCurrentUserStories(updateResult: SprintUpdateResult): Unit = {
    if (updateResult.importantBoardChange) {
      storiesRepo.saveCurrentUserStories(updateResult.updatedSprint)
      storiesRepo.cleanUnnecessaryStates()
    }
  }

  private def appendTasksEventsIfNecessary(updateResult: SprintUpdateResult): Unit = {
    if (updateResult.importantBoardChange)
      eventsRepo.appendTasksEvents(updateResult.newAddedEvents)
  }
}

object SprintRepository {
  def apply(sprintRoot: File, sprintId: String) = new SprintRepository(sprintRoot, sprintId)
}
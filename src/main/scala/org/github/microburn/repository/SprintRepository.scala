/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.repository

import java.io.File

import org.github.microburn.domain.{Sprint, SprintUpdateResult}

class SprintRepository(sprintRoot: File, sprintId: Int) {
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
    if (updateResult.importantBoardStateChange) {
      storiesRepo.saveCurrentUserStories(updateResult.updatedSprint)
      storiesRepo.cleanUnnecessaryStates()
    }
  }

  private def appendTasksEventsIfNecessary(updateResult: SprintUpdateResult): Unit = {
    if (updateResult.importantBoardStateChange)
      eventsRepo.appendTasksEvents(updateResult.newAddedEvents)
  }
}

object SprintRepository {
  def apply(sprintRoot: File, sprintId: Int) = new SprintRepository(sprintRoot, sprintId)
}
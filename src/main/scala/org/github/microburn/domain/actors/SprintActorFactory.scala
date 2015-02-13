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
package org.github.microburn.domain.actors

import java.io.File
import java.util.Date

import net.liftweb.actor.LiftActor
import org.github.microburn.domain._
import org.github.microburn.repository.SprintRepository

import scala.concurrent.duration.FiniteDuration

class SprintActorFactory(config: ProjectConfig, initialFetchToSprintStartAcceptableDelayMinutes: FiniteDuration, changeNotifyingActor: LiftActor) {
  private val baseDeterminer = new SprintBaseStateDeterminer(initialFetchToSprintStartAcceptableDelayMinutes)

  def fromRepo(sprintId: String): Option[SprintActor] = {
    val repo = createRepo(sprintId)
    repo.loadSprint.map { sprint =>
      new SprintActor(sprint)(repo, baseDeterminer, config, changeNotifyingActor)
    }
  }

  def migrateSprint(sprintId: String, majorDetails: MajorSprintDetails, userStories: Seq[UserStory]): SprintActor = {
    createSprint(sprintId, majorDetails, userStories, majorDetails.end)
  }

  def createSprint(sprintId: String, majorDetails: MajorSprintDetails, userStories: Seq[UserStory], timestamp: Date): SprintActor = {
    val sprint = Sprint.withEmptyEvents(sprintId, SprintDetails(majorDetails), BoardState(userStories.toIndexedSeq, timestamp))
    val repo = createRepo(sprintId)
    repo.saveSprint(sprint)
    new SprintActor(sprint)(repo, baseDeterminer, config, changeNotifyingActor)
  }

  private def createRepo(sprintId: String): SprintRepository = {
    val sprintRoot = new File(config.dataRoot, sprintId)
    SprintRepository(sprintRoot, sprintId)
  }
}
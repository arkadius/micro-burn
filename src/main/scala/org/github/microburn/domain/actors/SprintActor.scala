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

class SprintActor(var sprint: Sprint)
                 (repo: SprintRepository, config: ProjectConfig, changeNotifyingActor: LiftActor) extends LiftActor {

  implicit val configImplicit = config

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case GetDetails =>
      reply(sprint.details)
    case FinishSprint(sprintId, timestamp) =>
      require(sprintId == sprint.id)
      updateSprintAndReply(sprint.finish(timestamp))
    case RemoveSprint(sprintId, timestamp) =>
      require(sprintId == sprint.id)
      updateSprintAndReply(sprint.markRemoved(timestamp))
    case UpdateSprint(sprintId, userStories, finishSprint, timestamp) =>
      require(sprintId == sprint.id)
      updateSprintAndReply(sprint.update(userStories, finishSprint)(timestamp))
    case GetStoryPointsHistory(sprintId: String) =>
      require(sprintId == sprint.id)
      reply(SprintHistory(sprint.initialStoryPointsSum, sprint.initialStoryPointsNotDoneSum, sprint.initialDate, sprint.columnStatesHistory, sprint.details))
  }

  private def updateSprintAndReply(f: => SprintUpdateResult) = {
    val result = f
    sprint = result.updatedSprint
    repo.saveUpdateResult(result)
    if (result.importantBoardStateChange)
      changeNotifyingActor ! BoardStateChanged(sprint.id)
    reply(sprint.id)
  }

}

case class BoardStateChanged(sprintId: String)

case object GetDetails

class SprintActorFactory(config: ProjectConfig, changeNotifyingActor: LiftActor) {
  def fromRepo(sprintId: String): Option[SprintActor] = {
    val repo = createRepo(sprintId)
    repo.loadSprint.map { sprint =>
      new SprintActor(sprint)(repo, config, changeNotifyingActor)
    }
  }

  def migrateSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory]): SprintActor = {
    createSprint(sprintId, details, userStories, details.end)
  }

  def createSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory], timestamp: Date): SprintActor = {
    val sprint = Sprint.withEmptyEvents(sprintId, details, BoardState(userStories.toIndexedSeq, timestamp))
    val repo = createRepo(sprintId)
    repo.saveSprint(sprint)
    new SprintActor(sprint)(repo, config, changeNotifyingActor)
  }

  private def createRepo(sprintId: String): SprintRepository = {
    val sprintRoot = new File(config.dataRoot, sprintId)
    SprintRepository(sprintRoot, sprintId)
  }
}
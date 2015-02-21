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

import java.util.Date

import net.liftweb.actor.LiftActor
import org.github.microburn.domain._
import org.github.microburn.repository.SprintRepository

class SprintActor(var sprint: Sprint)
                 (repo: SprintRepository,
                  config: ProjectConfig,
                  changeNotifyingActor: LiftActor) extends LiftActor {

  implicit val configImplicit = config

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case GetDetails =>
      reply(DetailsWithBaseStoryPoints(sprint.details, sprint.baseStoryPointsForStart))
    case UpdateSprintDetails(sprintId, details, timestamp) =>
      require(sprintId == sprint.id)
      updateSprintAndReply(sprint.updateDetails(details)(timestamp))
    case UpdateSprint(sprintId, userStories, details, timestamp) =>
      require(sprintId == sprint.id)
      updateSprintAndReply(sprint.update(userStories, details)(timestamp))
    case GetStoryPointsHistory(sprintId: Int) =>
      require(sprintId == sprint.id)
      reply(sprint.sprintHistory)
  }


  private def updateSprintAndReply(f: => SprintUpdateResult) = {
    val result = f
    sprint = result.updatedSprint
    repo.saveUpdateResult(result)
    if (result.importantBoardStateChange)
      changeNotifyingActor ! BoardStateChanged(sprint.id)
    else if (result.importantDetailsChange)
      changeNotifyingActor ! SprintDetailsChanged(sprint.id)
    reply(sprint.id)
  }

}

case class BoardStateChanged(sprintId: Int)

case class SprintDetailsChanged(sprintId: Int)

case object GetDetails

case class DetailsWithBaseStoryPoints(details: SprintDetails, baseStoryPointsSum: BigDecimal)

case class UpdateSprintDetails(sprintId: Int, details: SprintDetails, timestamp: Date)

case class UpdateSprint(sprintId: Int, userStories: Seq[UserStory], details: MajorSprintDetails, timestamp: Date)

case class GetStoryPointsHistory(sprintId: Int)
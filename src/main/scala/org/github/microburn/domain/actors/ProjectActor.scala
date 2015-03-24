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

import net.liftmodules.ng.Angular.NgModel
import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.{Box, Failure, Full}
import net.liftweb.http.ListenerManager
import org.github.microburn.domain._
import org.github.microburn.repository.ProjectRepository
import org.github.microburn.util.logging.Slf4jLogging

import scala.collection.immutable.TreeMap

class ProjectActor(config: ProjectConfig)
  extends LiftActor with ListenerManager with Slf4jLogging {

  import org.github.microburn.util.concurrent.FutureEnrichments._

  private val sprintFactory = new SprintActorFactory(config, this)
  private val projectRepo = ProjectRepository(config.dataRoot)

  private var sprintActors: TreeMap[Int, SprintActor] =
    TreeMap(
      (for {
        sprintId <- projectRepo.sprintIds
        sprintActor <- sprintFactory.fromRepo(sprintId)
      } yield sprintId -> sprintActor): _*
    )

  override protected def lowPriority: PartialFunction[Any, Unit] = {
    case GetProjectState =>
      reply(prepareProjectState)
    case GetFullProjectState =>
      reply(prepareFullProjectState)
    case c@CreateNewSprint(sprintId, details, userStories, timestamp) if details.isFinished =>
      info(sprintId + ": " + details)
      val createResult = for {
        validatedSprint <- sprintFactory.migrateSprint(sprintId, details, userStories)
      } yield {
        sprintActors += sprintId -> validatedSprint
        updateListeners()
        sprintId
      }
      reply(createResult)
    case c@CreateNewSprint(sprintId, details, userStories, timestamp) =>
      info(sprintId + ": " + details)
      val createResult = for {
        validatedSprint <- sprintFactory.createSprint(sprintId, details, userStories, timestamp)
      } yield {
        sprintActors += sprintId -> validatedSprint
        updateListeners()
        sprintId
      }
      reply(createResult)
    case updateDetails: UpdateSprintDetails =>
      info(updateDetails.toString)
      val resultFuture = sprintActors(updateDetails.sprintId) !< updateDetails
      reply(resultFuture)
    case update: UpdateSprint =>
      val resultFuture = sprintActors(update.sprintId) !< update
      reply(resultFuture)
    case getHistory: GetStoryPointsHistory =>
      val future = sprintActors.get(getHistory.sprintId).map { sprintActor =>
        (sprintActor !< getHistory).map(Full(_))
      }.getOrElse(LAFuture[Box[_]](() => Failure("Sprint with given id does not exist")))
      reply(future)
    case detailsChanged: SprintDetailsChanged =>
      updateListeners()
  }

  override protected def createUpdate: Any = prepareProjectState

  private def prepareProjectState: LAFuture[ProjectState] = fetchDetails.map { sprints =>
    ProjectState(sprints.filterNot(_.isRemoved).map(_.toMajor))
  }

  private def prepareFullProjectState: LAFuture[FullProjectState] = fetchDetails.map { sprints =>
    FullProjectState(sprints)
  }

  private def fetchDetails: LAFuture[List[SprintIdWithDetails]] = {
    val sprintWithStateFutures = sprintActors.map {
      case (sprintId, sprintActor) =>
        (sprintActor !< GetDetails).mapTo[DetailsWithBaseStoryPoints] map { details =>
          SprintIdWithDetails(sprintId, details.details, details.baseStoryPoints.toDouble)
        }
    }.toSeq
    LAFuture.collect(sprintWithStateFutures : _*).map { sprints =>
      sprints.sortBy(_.id)
    }
  }
}

case object GetProjectState

case object GetFullProjectState

case class ProjectState(sprints: Seq[SprintIdWithMajorDetails]) extends NgModel {
  def sprintIds: Set[Int] = sprints.map(_.id).toSet
}

case class FullProjectState(sprints: Seq[SprintIdWithDetails])

case class SprintIdWithDetails(id: Int, details: SprintDetails, baseStoryPoints: Double) {
  def isActive: Boolean = details.isActive
  def isRemoved: Boolean = details.isRemoved
  def toMajor: SprintIdWithMajorDetails = SprintIdWithMajorDetails(id, details.toMajor, baseStoryPoints)
}

case class SprintIdWithMajorDetails(id: Int, details: MajorSprintDetails, baseStoryPoints: Double) {
  def isActive: Boolean = details.isActive
}

case class CreateNewSprint(sprintId: Int, details: MajorSprintDetails, userStories: Seq[UserStory], timestamp: Date)
package org.github.micoburn.domain.actors

import java.io.File
import java.util.Date

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.{Failure, Full, Box}
import org.github.micoburn.domain._
import org.github.micoburn.repository.ProjectRepository

import scalaz.Scalaz._

class ProjectActor(config: ProjectConfig, sprintChangeNotifyingActor: LiftActor) extends LiftActor {
  import org.github.micoburn.util.concurrent.FutureEnrichments._

  private val sprintFactory = new SprintActorFactory(config, sprintChangeNotifyingActor)
  private val projectRepo = ProjectRepository(config.dataRoot)

  private var sprintActors: Map[String, SprintActor] = (
    for {
      sprintRoot <- projectRepo.sprintRoots
      sprintId = sprintRoot.getName
      sprintActor <- sprintFactory.fromRepo(sprintId)
    } yield (sprintId, sprintActor)
  ).toMap

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case GetSprintsWithStates =>
      val sprintWithStateFutures = sprintActors.map {
        case (sprintId, sprintActor) =>
          (sprintActor !< IsActive).mapTo[Boolean] map { isActive =>
            SprintWithState(sprintId, isActive)
          }
      }.toSeq
      reply(collectWithWellEmptyListHandling(sprintWithStateFutures))
    case CreateNewSprint(sprintId, details, userStories, timestamp) if details.finished =>
      sprintActors += sprintId -> sprintFactory.migrateSprint(sprintId, details, userStories)
      reply(sprintId)
    case CreateNewSprint(sprintId, details, userStories, timestamp) =>
      sprintActors += sprintId -> sprintFactory.createSprint(sprintId, details, userStories, timestamp)
      reply(sprintId)
    case update: UpdateSprint =>
      reply(sprintActors(update.sprintId) !< update)
    case getHistory: GetStoryPointsHistory =>
      val future = sprintActors.get(getHistory.sprintId).map { sprintActor =>
        (sprintActor !< getHistory).map(Full(_))
      }.getOrElse(LAFuture[Box[_]](() => Failure("Sprint with given id does not exist")))
      reply(future)
  }
}

case object GetSprintsWithStates

case class SprintWithState(sprintId: String, isActive: Boolean)

case class CreateNewSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory], timestamp: Date)

case class UpdateSprint(sprintId: String, userStories: Seq[UserStory], finishSprint: Boolean, timestamp: Date)

case class GetStoryPointsHistory(sprintId: String)

case class SprintHistory(initialStoryPointsSum: Int,
                         initialDate: Date,
                         columnStates: Seq[DateWithColumnsState],
                         sprintDetails: SprintDetails)
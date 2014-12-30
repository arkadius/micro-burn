package org.github.microburn.domain.actors

import java.util.Date

import net.liftmodules.ng.Angular.NgModel
import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.http.ListenerManager
import org.github.microburn.domain._
import org.github.microburn.repository.ProjectRepository

class ProjectActor(config: ProjectConfig, sprintChangeNotifyingActor: LiftActor) extends LiftActor with ListenerManager {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  private val sprintFactory = new SprintActorFactory(config, sprintChangeNotifyingActor)
  private val projectRepo = ProjectRepository(config.dataRoot)

  private var sprintActors: Map[String, SprintActor] = (
    for {
      sprintRoot <- projectRepo.sprintRoots
      sprintId = sprintRoot.getName
      sprintActor <- sprintFactory.fromRepo(sprintId)
    } yield (sprintId, sprintActor)
  ).toMap

  override protected def lowPriority: PartialFunction[Any, Unit] = {
    case GetProjectState =>
      reply(prepareProjectState)
    case CreateNewSprint(sprintId, details, userStories, timestamp) if details.finished =>
      sprintActors += sprintId -> sprintFactory.migrateSprint(sprintId, details, userStories)
      updateListeners()
      reply(sprintId)
    case CreateNewSprint(sprintId, details, userStories, timestamp) =>
      sprintActors += sprintId -> sprintFactory.createSprint(sprintId, details, userStories, timestamp)
      updateListeners()
      reply(sprintId)
    case update: UpdateSprint =>
      if (update.detailsUpdated)
        updateListeners()
      reply(sprintActors(update.sprintId) !< update)
    case getHistory: GetStoryPointsHistory =>
      val future = sprintActors.get(getHistory.sprintId).map { sprintActor =>
        (sprintActor !< getHistory).map(Full(_))
      }.getOrElse(LAFuture[Box[_]](() => Failure("Sprint with given id does not exist")))
      reply(future)
  }

  override protected def createUpdate: Any = prepareProjectState

  private def prepareProjectState: LAFuture[ProjectState] = {
    val sprintWithStateFutures = sprintActors.map {
      case (sprintId, sprintActor) =>
        (sprintActor !< GetDetails).mapTo[SprintDetails] map { details =>
          SprintWithDetails(sprintId, details)
        }
    }.toSeq
    collectWithWellEmptyListHandling(sprintWithStateFutures).map { sprints =>
//      Thread.sleep(5000)
      ProjectState(sprints.sortBy(_.details.start))
    }
  }
}

case object GetProjectState

case class ProjectState(sprints: Seq[SprintWithDetails]) extends NgModel {
  def sprintIds: Set[String] = sprints.map(_.sprintId).toSet
}

case class SprintWithDetails(sprintId: String, details: SprintDetails) {
  def isActive: Boolean = details.isActive
}

case class CreateNewSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory], timestamp: Date)

case class UpdateSprint(sprintId: String, userStories: Seq[UserStory], finishSprint: Boolean, timestamp: Date) {
  def detailsUpdated: Boolean = finishSprint
}

case class GetStoryPointsHistory(sprintId: String)

case class SprintHistory(initialStoryPointsSum: Int,
                         initialDate: Date,
                         columnStates: Seq[DateWithColumnsState],
                         sprintDetails: SprintDetails)
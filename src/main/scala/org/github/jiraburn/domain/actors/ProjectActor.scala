package org.github.jiraburn.domain.actors

import java.io.File
import java.util.Date

import org.github.jiraburn.domain.{ProjectConfig, DateWithStoryPoints, SprintDetails, UserStory}
import org.github.jiraburn.repository.ProjectRepository
import com.typesafe.config.ConfigFactory
import net.liftweb.actor.{LAFuture, LiftActor}
import org.github.jiraburn.util.concurrent.FutureEnrichments

class ProjectActor(projectRoot: File, config: ProjectConfig, sprintChangeNotifyingActor: LiftActor) extends LiftActor {
  import FutureEnrichments._

  private val sprintFactory = new SprintActorFactory(projectRoot, config, sprintChangeNotifyingActor)
  private val projectRepo = ProjectRepository(projectRoot)

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
      reply(LAFuture.collect(sprintWithStateFutures : _*))
    case CreateNewSprint(sprintId, details, userStories, timestamp) =>
      sprintActors += sprintId -> sprintFactory.createSprint(sprintId, details, userStories, timestamp)
      reply(Unit)
    case update: UpdateSprint =>
      reply(sprintActors(update.sprintId) !< update)
    case getHistory: GetStoryPointsHistory =>
      reply(sprintActors(getHistory.sprintId) !< getHistory)
    case Close =>
      val sprintClosedFutures = sprintActors.values.map { sprintActor =>
        (sprintActor !< Close).mapTo[Unit]
      }.toSeq
      reply(LAFuture.collect(sprintClosedFutures : _*))
  }
}

case object GetSprintsWithStates

case class SprintWithState(sprintId: String, isActive: Boolean)

case class CreateNewSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory], timestamp: Date)

case class UpdateSprint(sprintId: String, userStories: Seq[UserStory], finishSprint: Boolean, timestamp: Date)

case class GetStoryPointsHistory(sprintId: String)

case class StoryPointsHistory(initialStoryPoints: Int, history: Seq[DateWithStoryPoints])

case object Close

object ProjectActor {
  def apply(sprintChangeNotifyingActor: LiftActor): ProjectActor = {
    val config = ConfigFactory.load()
    val projectRoot = new File(config.getString("data.project.root"))
    val projectConfig = ProjectConfig(config)
    new ProjectActor(projectRoot, projectConfig, sprintChangeNotifyingActor)
  }
}
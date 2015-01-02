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
    case UpdateSprint(sprintId, userStories, finishSprint, timestamp) =>
      require(sprintId == sprint.id)
      val result = sprint.update(userStories, finishSprint)(timestamp)
      sprint = result.updatedSprint
      repo.saveUpdateResult(result)
      if (result.importantBoardStateChange)
        changeNotifyingActor ! BoardStateChanged(sprint.id)
      reply(sprint.id)
    case GetStoryPointsHistory(sprintId: String) =>
      require(sprintId == sprint.id)
      reply(SprintHistory(sprint.initialStoryPointsSum, sprint.initialDate, sprint.columnStatesHistory, sprint.details))
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
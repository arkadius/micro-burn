package org.github.jiraburn.domain.actors

import java.io.File
import java.util.Date

import net.liftweb.actor.LiftActor
import org.github.jiraburn.domain._
import org.github.jiraburn.repository.SprintRepository

class SprintActor(var sprint: Sprint)
                 (repo: SprintRepository, config: ProjectConfig, changeNotifyingActor: LiftActor) extends LiftActor {

  implicit val configImplicit = config

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case IsActive =>
      reply(sprint.isActive)
    case UpdateSprint(sprintId, userStories, finishSprint, timestamp) =>
      require(sprintId == sprint.id)
      val result = sprint.update(userStories, finishSprint)(timestamp)
      sprint = result.updatedSprint
      repo.saveUpdateResult(result)
      if (result.importantChange)
        changeNotifyingActor ! SprintChanged(sprint.id)
      reply(sprint.id)
    case GetStoryPointsHistory(sprintId: String) =>
      require(sprintId == sprint.id)
      reply(SprintHistory(sprint.initialStoryPointsSum, sprint.initialDate, sprint.columnStatesHistory, sprint.details))
  }
}

case object IsActive

case class SprintChanged(sprintId: String)

class SprintActorFactory(projectRoot: File, config: ProjectConfig, changeNotifyingActor: LiftActor) {
  def fromRepo(sprintId: String): Option[SprintActor] = {
    val repo = createRepo(sprintId)
    repo.loadSprint.map { sprint =>
      new SprintActor(sprint)(repo, config, changeNotifyingActor)
    }
  }

  def migrateSprint(sprintId: String, details: SprintDetails, userStories: Set[UserStory]): SprintActor = {
    createSprint(sprintId, details, userStories, details.end)
  }

  def createSprint(sprintId: String, details: SprintDetails, userStories: Set[UserStory], timestamp: Date): SprintActor = {
    val sprint = Sprint.withEmptyEvents(sprintId, details, BoardState(userStories, timestamp))
    val repo = createRepo(sprintId)
    repo.saveSprint(sprint)
    new SprintActor(sprint)(repo, config, changeNotifyingActor)
  }

  private def createRepo(sprintId: String): SprintRepository = {
    val sprintRoot = new File(projectRoot, sprintId)
    SprintRepository(sprintRoot, sprintId)
  }
}
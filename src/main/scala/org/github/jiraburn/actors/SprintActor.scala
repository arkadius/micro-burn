package org.github.jiraburn.actors

import java.io.File

import org.github.jiraburn.domain.{ProjectConfig, Sprint, SprintDetails, UserStory}
import org.github.jiraburn.repository.SprintRepository
import net.liftweb.actor.LiftActor

class SprintActor(var sprint: Sprint,
                  var repo: SprintRepository)
                 (config: ProjectConfig, changeNotifyingActor: LiftActor) extends LiftActor {

  implicit val configImplicit = config

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case IsActive =>
      reply(sprint.isActive)
    case UpdateSprint(sprintId, userStories, finishSprint, timestamp) =>
      require(sprintId == sprint.id)
      val result = sprint.update(userStories, finishSprint)(timestamp)
      sprint = result.updatedSprint
      repo = repo.saveUpdateResult(result)
      if (result.importantChange)
        changeNotifyingActor ! SprintChanged(sprint.id)
    case GetStoryPointsHistory(sprintId: String) =>
      require(sprintId == sprint.id)
      reply(StoryPointsHistory(sprint.initialStoryPoints, sprint.storyPointsChanges))
    case Close =>
      repo.flush()
      reply(Unit)
  }

}

case object IsActive

case class SprintChanged(sprintId: String)

class SprintActorFactory(projectRoot: File, config: ProjectConfig, changeNotifyingActor: LiftActor) {
  def fromRepo(sprintId: String): Option[SprintActor] = {
    val repo = createRepo(sprintId)
    repo.loadSprint.map { sprint =>
      new SprintActor(sprint, repo)(config, changeNotifyingActor)
    }
  }

  def createSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory]): SprintActor = {
    val sprint = Sprint.withEmptyEvents(sprintId, details, userStories)
    val repo = createRepo(sprintId)
    repo.saveSprint(sprint)
    new SprintActor(sprint, repo)(config, changeNotifyingActor)
  }

  private def createRepo(sprintId: String): SprintRepository = {
    val sprintRoot = new File(projectRoot, sprintId)
    SprintRepository(sprintRoot, sprintId)
  }
}
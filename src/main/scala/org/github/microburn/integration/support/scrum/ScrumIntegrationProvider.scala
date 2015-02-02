package org.github.microburn.integration.support.scrum

import java.util.Date

import net.liftweb.actor.{LAFuture, LiftActor}
import org.github.microburn.domain.{UserStory, SprintDetails}
import org.github.microburn.domain.actors.{GetProjectState, UpdateSprint, CreateNewSprint, ProjectState}
import org.github.microburn.integration.IntegrationProvider

class ScrumIntegrationProvider(sprintsProvider: SprintsDataProvider, tasksProvider: TasksDataProvider)(projectActor: LiftActor)
  extends IntegrationProvider {

  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._

  override def updateProject(): LAFuture[_] = {
    implicit val timestamp = new Date
    for {
      (currentSprints, updatedSprintIds) <- parallelCurrentAndUpdatedSprints
      _ <- parallelCreateAndUpdate(currentSprints, updatedSprintIds)
    } yield Unit
  }

  private def parallelCurrentAndUpdatedSprints: LAFuture[(ProjectState, Seq[Long])] = {
    val currentStateFuture = (projectActor ?? GetProjectState).mapTo[ProjectState]
      .withLoggingFinished("current sprint ids: " + _.sprints.map(_.id).mkString(", "))
    val updatedIdsFuture = sprintsProvider.allSprintIds.withLoggingFinished("updated sprints ids: " + _.mkString(", "))
    for {
      currentState <- currentStateFuture
      updatedIds <- updatedIdsFuture
    } yield (currentState, updatedIds)
  }

  private def parallelCreateAndUpdate(current: ProjectState, updatedSprintIds: Seq[Long])
                                     (implicit timestamp: Date): LAFuture[_] = {
    val createResultFuture = createNewSprints(current, updatedSprintIds).withLoggingFinished("created sprints: " + _.mkString(", "))
    val updateResultFuture = updateActiveSprints(current).withLoggingFinished("updated sprints: " + _.mkString(", "))
    for {
      _ <- createResultFuture
      _ <- updateResultFuture
    } yield Unit
  }

  private def createNewSprints(current: ProjectState, retrieved: Seq[Long])
                              (implicit timestamp: Date): LAFuture[List[String]] = {
    val currentIds = current.sprintIds
    val missing = retrieved.filterNot { l => currentIds.contains(l.toString) }
    val createResults = missing.map { sprintId =>
      for {
        (details, userStories) <- parallelSprintDetailsAndUserStories(sprintId.toString)
        createResult <- (projectActor !< CreateNewSprint(sprintId.toString, details, userStories, timestamp)).mapTo[String]
      } yield createResult
    }
    LAFuture.collect(createResults : _*)
  }

  private def updateActiveSprints(current: ProjectState)
                                 (implicit timestamp: Date): LAFuture[List[String]] = {
    val updateResults = current.sprints.collect {
      case withDetails if withDetails.isActive =>
        for {
          (details, userStories) <- parallelSprintDetailsAndUserStories(withDetails.id)
          updateResult <- (projectActor ?? UpdateSprint(withDetails.id, userStories, !details.isActive, timestamp)).mapTo[String]
        } yield updateResult
    }
    LAFuture.collect(updateResults : _*)
  }

  private def parallelSprintDetailsAndUserStories(sprintId: String): LAFuture[(SprintDetails, Seq[UserStory])] = {
    val detailsFuture = sprintsProvider.sprintDetails(sprintId).withLoggingFinished(s"sprint details for sprint $sprintId: " + _)
    val tasksFuture = tasksProvider.userStories(sprintId).withLoggingFinished(s"user stories count for sprint $sprintId: " + _.size)
    for {
      details <- detailsFuture
      tasks <- tasksFuture
    } yield (details, tasks)
  }

}

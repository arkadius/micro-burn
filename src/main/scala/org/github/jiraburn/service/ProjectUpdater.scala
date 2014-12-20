package org.github.jiraburn.service

import java.util.Date

import net.liftweb.actor.{LAFuture, LiftActor}
import org.github.jiraburn.domain.actors._
import org.github.jiraburn.domain.{SprintDetails, UserStory}
import org.github.jiraburn.jira.{SprintsDataProvider, TasksDataProvider}

class ProjectUpdater(projectActor: LiftActor, sprintsProvider: SprintsDataProvider, tasksProvider: TasksDataProvider) {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._
  import org.github.jiraburn.util.concurrent.LiftActorEnrichments._

  def updateProject(): LAFuture[_] = {
    implicit val timestamp = new Date
    for {
      (currentSprints, futureSprintIds) <- parallelCurrentAndFutureSprints
      _ <- parallelCreateAndUpdate(currentSprints, futureSprintIds)
    } yield Unit
  }

  private def parallelCurrentAndFutureSprints: LAFuture[(Seq[SprintWithState], Seq[Long])] = {
    val currentFuture = (projectActor !< GetSprintsWithStates).mapTo[Seq[SprintWithState]]
    val futureIdsFuture = sprintsProvider.allSprintIds
    for {
      current <- currentFuture
      futureIds <- futureIdsFuture
    } yield (current, futureIds)
  }

  private def parallelCreateAndUpdate(currentSprints: Seq[SprintWithState], futureSprintIds: Seq[Long])
                                     (implicit timestamp: Date): LAFuture[_] = {
    val createResultFuture = createNewSprints(currentSprints, futureSprintIds)
    val updateResultFuture = updateActiveSprints(currentSprints)
    for {
      _ <- createResultFuture
      _ <- updateResultFuture
    } yield Unit
  }

  private def createNewSprints(current: Seq[SprintWithState], retrieved: Seq[Long])
                              (implicit timestamp: Date): LAFuture[_] = {
    val currentIds = current.map(_.sprintId).toSet
    val missing = retrieved.filterNot { l => currentIds.contains(l.toString) }
    val createResults = missing.map { sprintId =>
      for {
        (details, userStories) <- parallelSprintDetailsAndUserStories(sprintId.toString)
        createResult <- projectActor !< CreateNewSprint(sprintId.toString, details, userStories, timestamp)
      } yield createResult
    }
    LAFuture.collect(createResults : _*)
  }

  private def updateActiveSprints(current: Seq[SprintWithState])
                                 (implicit timestamp: Date): LAFuture[_] = {
    val updateResults = current.collect {
      case SprintWithState(sprintId, true) =>
        for {
          (details, userStories) <- parallelSprintDetailsAndUserStories(sprintId)
          updateResult <- projectActor ?? UpdateSprint(sprintId, userStories, !details.isActive, timestamp)
        } yield updateResult
    }
    LAFuture.collect(updateResults : _*)
  }

  private def parallelSprintDetailsAndUserStories(sprintId: String): LAFuture[(SprintDetails, Seq[UserStory])] = {
    val detailsFuture = sprintsProvider.sprintDetails(sprintId)
    val tasksFuture = tasksProvider.userStories(sprintId)
    for {
      details <- detailsFuture
      tasks <- tasksFuture
    } yield (details, tasks)
  }

}
package org.github.microburn.service

import java.util.Date

import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.Failure
import net.liftweb.util.Schedule
import org.github.microburn.domain.actors._
import org.github.microburn.domain.{SprintDetails, UserStory}
import org.github.microburn.integration.{SprintsDataProvider, TasksDataProvider}
import org.github.microburn.util.logging.Slf4jLogging
import org.joda.time.Seconds

class ProjectUpdater(projectActor: LiftActor, providers: IntegrationProviders, updatePeriodSeconds: Int) extends Slf4jLogging {

  import net.liftweb.util.Helpers.TimeSpan._
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._

  def start(): Unit = repeat()

  private def repeat(): Unit = {
    updateProject().onComplete { result =>
      result match {
        case Failure(msg, ex, _) => error(s"Error while updating project data: ${ex.map(_.getMessage).openOr(msg)}")
        case _ =>
      }
      Schedule.schedule(() => repeat(), Seconds.seconds(updatePeriodSeconds).toPeriod)
    }
  }

  def updateProject(): LAFuture[_] = {
    implicit val timestamp = new Date
    for {
      (currentSprints, updatedSprintIds) <- parallelCurrentAndUpdatedSprints
      _ <- parallelCreateAndUpdate(currentSprints, updatedSprintIds)
    } yield Unit
  }

  private def parallelCurrentAndUpdatedSprints: LAFuture[(ProjectState, Seq[Long])] = {
    val currentStateFuture = (projectActor ?? GetProjectState).mapTo[ProjectState]
      .withLoggingFinished("current sprint ids: " + _.sprints.map(_.sprintId).mkString(", "))
    val updatedIdsFuture = providers.sprintsProvider.allSprintIds.withLoggingFinished("updated sprints ids: " + _.mkString(", "))
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
    collectWithWellEmptyListHandling(createResults)
  }

  private def updateActiveSprints(current: ProjectState)
                                 (implicit timestamp: Date): LAFuture[List[String]] = {
    val updateResults = current.sprints.collect {
      case withDetails if withDetails.isActive =>
        for {
          (details, userStories) <- parallelSprintDetailsAndUserStories(withDetails.sprintId)
          updateResult <- (projectActor ?? UpdateSprint(withDetails.sprintId, userStories, !details.isActive, timestamp)).mapTo[String]
        } yield updateResult
    }
    collectWithWellEmptyListHandling(updateResults)
  }

  private def parallelSprintDetailsAndUserStories(sprintId: String): LAFuture[(SprintDetails, Seq[UserStory])] = {
    val detailsFuture = providers.sprintsProvider.sprintDetails(sprintId).withLoggingFinished(s"sprint details for sprint $sprintId: " + _)
    val tasksFuture = providers.tasksProvider.userStories(sprintId).withLoggingFinished(s"user stories count for sprint $sprintId: " + _.size)
    for {
      details <- detailsFuture
      tasks <- tasksFuture
    } yield (details, tasks)
  }

}

case class IntegrationProviders(sprintsProvider: SprintsDataProvider, tasksProvider: TasksDataProvider)
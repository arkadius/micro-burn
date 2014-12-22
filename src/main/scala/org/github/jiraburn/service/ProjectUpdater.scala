package org.github.jiraburn.service

import java.util.Date

import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.Failure
import net.liftweb.util.Schedule
import org.github.jiraburn.domain.actors._
import org.github.jiraburn.domain.{SprintDetails, UserStory}
import org.github.jiraburn.jira.{SprintsDataProvider, TasksDataProvider}
import org.github.jiraburn.util.logging.Slf4jLogging
import org.joda.time.{Seconds, Period, Duration}

class ProjectUpdater(projectActor: LiftActor, sprintsProvider: SprintsDataProvider, tasksProvider: TasksDataProvider, jiraFetchPeriodSeconds: Int) extends Slf4jLogging {

  import net.liftweb.util.Helpers.TimeSpan._
  import org.github.jiraburn.util.concurrent.FutureEnrichments._
  import org.github.jiraburn.util.concurrent.LiftActorEnrichments._

  def start(): Unit = repeat()

  private def repeat(): Unit = {
    updateProject().onComplete { result =>
      result match {
        case Failure(msg, ex, _) => error(s"Error while updating project data: ${ex.map(_.getMessage).openOr(msg)}")
        case _ =>
      }
      Schedule.schedule(() => repeat(), Seconds.seconds(jiraFetchPeriodSeconds).toPeriod)
    }
  }

  def updateProject(): LAFuture[_] = {
    implicit val timestamp = new Date
    for {
      (currentSprints, updatedSprintIds) <- parallelCurrentAndUpdatedSprints
      _ <- parallelCreateAndUpdate(currentSprints, updatedSprintIds)
    } yield Unit
  }

  private def parallelCurrentAndUpdatedSprints: LAFuture[(Seq[SprintWithState], Seq[Long])] = {
    val currentFuture = (projectActor ?? GetSprintsWithStates)
      .mapTo[Seq[SprintWithState]]
      .withLoggingFinished("current sprint ids: " + _.map(_.sprintId).mkString(", "))
    val updatedIdsFuture = sprintsProvider.allSprintIds.withLoggingFinished("updated sprints ids: " + _.mkString(", "))
    for {
      current <- currentFuture
      updatedIds <- updatedIdsFuture
    } yield (current, updatedIds)
  }

  private def parallelCreateAndUpdate(currentSprints: Seq[SprintWithState], updatedSprintIds: Seq[Long])
                                     (implicit timestamp: Date): LAFuture[_] = {
    val createResultFuture = createNewSprints(currentSprints, updatedSprintIds).withLoggingFinished("created sprints: " + _.mkString(", "))
    val updateResultFuture = updateActiveSprints(currentSprints).withLoggingFinished("updated sprints: " + _.mkString(", "))
    for {
      _ <- createResultFuture
      _ <- updateResultFuture
    } yield Unit
  }

  private def createNewSprints(current: Seq[SprintWithState], retrieved: Seq[Long])
                              (implicit timestamp: Date): LAFuture[List[String]] = {
    val currentIds = current.map(_.sprintId).toSet
    val missing = retrieved.filterNot { l => currentIds.contains(l.toString) }
    val createResults = missing.map { sprintId =>
      for {
        (details, userStories) <- parallelSprintDetailsAndUserStories(sprintId.toString)
        createResult <- (projectActor !< CreateNewSprint(sprintId.toString, details, userStories, timestamp)).mapTo[String]
      } yield createResult
    }
    collectWithWellEmptyListHandling(createResults)
  }

  private def updateActiveSprints(current: Seq[SprintWithState])
                                 (implicit timestamp: Date): LAFuture[List[String]] = {
    val updateResults = current.collect {
      case SprintWithState(sprintId, true) =>
        for {
          (details, userStories) <- parallelSprintDetailsAndUserStories(sprintId)
          updateResult <- (projectActor ?? UpdateSprint(sprintId, userStories, !details.isActive, timestamp)).mapTo[String]
        } yield updateResult
    }
    collectWithWellEmptyListHandling(updateResults)
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
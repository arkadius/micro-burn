package org.github.jiraburn.service

import java.util.Date

import net.liftweb.actor.LiftActor
import org.github.jiraburn.domain.actors._
import org.github.jiraburn.jira.SprintsProvider
import org.github.jiraburn.util.concurrent.FutureEnrichments

class ProjectUpdater(projectActor: LiftActor, sprintsProvider: SprintsProvider) {
  import FutureEnrichments._

  def updateProject() = {
    implicit val timestamp = new Date
    for {
      currentSprints <- (projectActor !< GetSprintsWithStates).mapTo[Seq[SprintWithState]]
      retrievedSprints <- sprintsProvider.allSprintIds
    } {
      createNewSprints(currentSprints, retrievedSprints)
      updateActiveSprints(currentSprints)
    }
  }

  private def createNewSprints(current: Seq[SprintWithState], retrieved: List[Long])
                              (implicit timestamp: Date): Unit = {
    val currentIds = current.map(_.sprintId).toSet
    val missing = retrieved.filterNot { l => currentIds.contains(l.toString) }
    missing.map { sprintId =>
      sprintsProvider.sprintDetails(sprintId.toString).map { details =>
        projectActor ! CreateNewSprint(sprintId.toString, details, null, timestamp) // TODO: tasks
      }
    }
  }


  private def updateActiveSprints(current: Seq[SprintWithState])
                                 (implicit timestamp: Date) = {
    current.collect {
      case SprintWithState(sprintId, true) =>
        sprintsProvider.sprintDetails(sprintId).map { details =>
          projectActor ! UpdateSprint(sprintId, null, !details.isActive, timestamp) // TODO: tasks
        }
    }
  }

}
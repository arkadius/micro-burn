/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.integration.support.kanban

import java.util.Date

import net.liftmodules.ng.Angular.NgModel
import net.liftweb.actor.{LAFuture, LiftActor}
import org.github.microburn.domain.{UserStory, SprintDetails}
import org.github.microburn.domain.actors._

import scala.concurrent.duration.FiniteDuration
import scalaz._
import Scalaz._

class ScrumSimulator(boardStateProvider: BoardStateProvider, projectActor: ProjectActor)
                    (initializationTimeout: FiniteDuration) extends LiftActor {

  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._

  private var currentSprintsInfo: Option[SprintInfo] = None
  
  this ! Init

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case Init =>
      val lastSprintInfoFuture = for {
        projectState <- (projectActor ?? GetProjectState).mapTo[ProjectState]
      } yield {
        optionalLastNumericalSprint(projectState.sprints)
      }
      // czekamy, żeby poprawnie wyliczy się id zanim ktoś zdąży wykonać inną akcję
      currentSprintsInfo = lastSprintInfoFuture.await(initializationTimeout)
    case FetchCurrentSprintsBoardState =>
      val fetchedStateFuture = currentSprintsInfo.filter(_.isActive).map { sprintsInfo =>
        boardStateProvider.currentUserStories.map { userStories =>
          FetchedBoardState(sprintsInfo, userStories)
        }  
      }.toFutureOfOption 
      reply(fetchedStateFuture)
    case StartSprint(name, start, end) =>
      val startFuture =
        if (currentSprintsInfo.exists(_.isActive))
          LAFuture(() => throw new IllegalStateException("You must finish current sprint before start new"))
        else
          doStartSprint(name, start, end)
      reply(startFuture)
    case NextSprint(details, userStories) =>
      val sprintInfo = currentSprintsInfo.map(_.next(details)).getOrElse(SprintInfo.zero(details))
      currentSprintsInfo = Some(sprintInfo)
      reply(projectActor !< CreateNewSprint(sprintInfo.id.toString, details, userStories, new Date))
    case DoFinishSprint(id) =>
      val finishFuture = currentSprintsInfo.filter(f => f.isActive || f.id.toString != id).map { sprintsInfo =>
        val finishedSprintInfo = sprintsInfo.finish
        currentSprintsInfo = Some(finishedSprintInfo)
        projectActor ?? FinishSprint(finishedSprintInfo.id.toString, new Date)
      }.getOrElse(LAFuture(() => throw new IllegalArgumentException("You can finish only current active sprint")))
      reply(finishFuture)
    case DoRemoveSprint(id) =>
      val removeFuture = if (currentSprintsInfo.exists(f => f.id.toString == id && f.isActive)) {
        LAFuture(() => throw new IllegalArgumentException("You cannot remove active sprint"))
      } else {
        currentSprintsInfo = currentSprintsInfo.flatMap { sprintInfo =>
          if (sprintInfo.id.toString == id)
            None
          else
            Some(sprintInfo)
        }
        projectActor ?? RemoveSprint(id, new Date)
      }
      reply(removeFuture)
  }

  private def doStartSprint(name: String, start: Date, end: Date): LAFuture[Any] = {
    for {
      userStories <- boardStateProvider.currentUserStories
      details = SprintDetails(name, start, end)
      // wysyłamy do siebie, żeby mieć pewność, że fetch będzie miał dobry currentSprintsInfo
      createResult <- this ?? NextSprint(details, userStories)
    } yield createResult
  }

  private def optionalLastNumericalSprint(sprints: Seq[SprintWithDetails]): Option[SprintInfo] = {
    sprints.flatMap { sprintWithDetails =>
      sprintWithDetails.id.parseInt.toOption.map { numericalId =>
        numericalId -> sprintWithDetails.details
      }
    }.sortBy {
      case (numericalId, details) => numericalId
    }.lastOption.map {
      case (numericalId, details) => SprintInfo(numericalId, details)
    }
  }

  private case object Init

  private case class NextSprint(details: SprintDetails, userStories: Seq[UserStory])
}

case class SprintInfo(id: Int, details: SprintDetails) {
  def isActive: Boolean = details.isActive
  
  def next(details: SprintDetails): SprintInfo = {
    SprintInfo(id + 1, details)
  }

  def finish: SprintInfo = copy(details = details.finish)
}

object SprintInfo {
  def zero(details: SprintDetails): SprintInfo = SprintInfo(0, details)
}

case object FetchCurrentSprintsBoardState

case class FetchedBoardState(sprintId: String, details: SprintDetails, userStories: Seq[UserStory]) {
  override def toString: String = s"id: $sprintId, details: $details, user stories count: ${userStories.size}"
}

object FetchedBoardState {
  def apply(info: SprintInfo, userStories: Seq[UserStory]): FetchedBoardState = {
    FetchedBoardState(info.id.toString, info.details, userStories)
  }
}

case class StartSprint(name: String, start: Date, end: Date) extends NgModel

case class DoFinishSprint(id: String)

case class DoRemoveSprint(id: String)

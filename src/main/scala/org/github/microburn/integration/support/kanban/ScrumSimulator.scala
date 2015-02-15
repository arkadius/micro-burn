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
import net.liftweb.common._
import org.github.microburn.domain.actors._
import org.github.microburn.domain.{SprintDetails, MajorSprintDetails, UserStory}

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.FiniteDuration
import scalaz.Scalaz._

class ScrumSimulator(boardStateProvider: BoardStateProvider, projectActor: ProjectActor)
                    (initializationTimeout: FiniteDuration) extends LiftActor {

  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._
  import org.github.microburn.util.concurrent.BoxEnrichments._

  private var currentSprints: TreeMap[Int, SprintDetails] = TreeMap.empty
  
  this ! Init

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case Init =>
      val lastSprints = for {
        projectState <- (projectActor ?? GetFullProjectState).mapTo[FullProjectState]
      } yield {
        val sprintsSeq = projectState.sprints.flatMap {
          case SprintIdWithDetails(sprintId, details, _) =>
            sprintId.parseInt.toOption.map { numericalId =>
              (numericalId, details)
            }
        }
        TreeMap(sprintsSeq: _*)
      }
      // czekamy, żeby poprawnie zostaną pobrane sprinty zanim ktoś zdąży wykonać inną akcję
      currentSprints = lastSprints.await(initializationTimeout)
    case FetchCurrentSprintsBoardState =>
      val fetchedStateFuture = lastActive.toOption.map { last =>
        boardStateProvider.currentUserStories.map { userStories =>
          FetchedBoardState(last, userStories)
        }  
      }.toFutureOfOption
      reply(fetchedStateFuture)
    case StartSprint(name, start, end) =>
      val startFuture =
        if (lastActive.isDefined)
          LAFuture[Box[Any]](() => throw new IllegalArgumentException("You must finish current sprint before start new"))
        else
          doStartSprint(name, start, end)
      reply(startFuture)
    case NextSprint(major, userStories) =>
      val future = (for {
        validatedTullDetails <- SprintDetails.create(major)
      } yield {
        val next = optionalLastNumericalSprint.map(_.next(validatedTullDetails)).getOrElse(NumericalSprintIdWithDetails.zero(validatedTullDetails))
        currentSprints += next.numericalId -> next.details
        projectActor !< CreateNewSprint(next.id, major, userStories, new Date)
      }).toFutureOfBox
      reply(future)
    case FinishSprint(sprintId) =>
      reply(updateSprintDetails(sprintId, _.finish))
    case RemoveSprint(sprintId) =>
      reply(updateSprintDetails(sprintId, _.markRemoved))
    case UpdateStartDate(sprintId, start) =>
      reply(updateSprintDetails(sprintId, _.updateStartDate(start)))
    case UpdateEndDate(sprintId, end) =>
      reply(updateSprintDetails(sprintId, _.updateEndDate(end)))
    case DefineBaseStoryPoints(sprintId, base) =>
      reply(updateSprintDetails(sprintId, _.defineBaseStoryPoints(BigDecimal(base))))
  }
  
  private def lastActive: Box[NumericalSprintIdWithDetails] = {
    optionalLastNumericalSprint.filter(_.isActive)
  }
  
  private def optionalLastNumericalSprint: Box[NumericalSprintIdWithDetails] = {
    currentSprints.lastOption.map(NumericalSprintIdWithDetails.apply _ tupled)
  }

  private def doStartSprint(name: String, start: Date, end: Date): LAFuture[Any] = {
    for {
      userStories <- boardStateProvider.currentUserStories
      details = MajorSprintDetails(name, start, end)
      // wysyłamy do siebie, żeby mieć pewność, że fetch będzie miał dobry currentSprintsInfo
      createResult <- this ?? NextSprint(details, userStories)
    } yield createResult
  }

  private def updateSprintDetails(sprintId: String, f: SprintDetails => Box[SprintDetails]): LAFuture[Box[Any]] = {
    (for {
      numericalSprintId <- sprintId.parseInt.toBox
      details <- currentSprints.get(numericalSprintId).toBox or
        Failure(s"Cannot find sprint with given id $numericalSprintId")
      updatedDetails <- f(details)
    } yield {
      currentSprints = currentSprints.updated(numericalSprintId, updatedDetails)
      projectActor ?? UpdateSprintDetails(numericalSprintId.toString, updatedDetails, new Date)
    }).toFutureOfBox
  }
  
  private case object Init

  private case class NextSprint(details: MajorSprintDetails, userStories: Seq[UserStory])
}

case class NumericalSprintIdWithDetails(numericalId: Int, details: SprintDetails) {
  def id: String = numericalId.toString
  
  def isActive: Boolean = details.isActive
  
  def next(details: SprintDetails): NumericalSprintIdWithDetails = {
    NumericalSprintIdWithDetails(numericalId + 1, details)
  }
}

object NumericalSprintIdWithDetails {
  def zero(details: SprintDetails): NumericalSprintIdWithDetails = NumericalSprintIdWithDetails(0, details)

  implicit val byIdOrdering: Ordering[NumericalSprintIdWithDetails] = Ordering.by(_.id)
}

case object FetchCurrentSprintsBoardState

case class FetchedBoardState(sprintId: String, details: MajorSprintDetails, userStories: Seq[UserStory]) {
  override def toString: String = s"id: $sprintId, details: $details, user stories count: ${userStories.size}"
}

object FetchedBoardState {
  def apply(idWithDetails: NumericalSprintIdWithDetails, userStories: Seq[UserStory]): FetchedBoardState = {
    FetchedBoardState(idWithDetails.id, idWithDetails.details.toMajor, userStories)
  }
}

case class StartSprint(name: String, start: Date, end: Date) extends NgModel

case class FinishSprint(id: String)

case class RemoveSprint(id: String)

case class UpdateStartDate(id: String, startDate: Date) extends NgModel

case class UpdateEndDate(id: String, endDate: Date) extends NgModel

case class DefineBaseStoryPoints(id: String, baseStoryPoints: Double) extends NgModel
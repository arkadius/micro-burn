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

import net.liftweb.actor.LAFuture
import org.github.microburn.domain.actors.{UpdateSprint, ProjectActor}
import org.github.microburn.integration.IntegrationProvider

import scala.concurrent.duration.FiniteDuration

class KanbanIntegrationProvider(boardStateProvider: BoardStateProvider, initializationTimeout: FiniteDuration)
                               (projectActor: ProjectActor)
  extends IntegrationProvider
  with ScrumSimulation {

  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._

  override def updateProject(implicit timestamp: Date): LAFuture[_] = {
    for {
      fetchedCurrentSprintsBoardState <- (scrumSimulator ?? FetchCurrentSprintsBoardState)
        .mapTo[Option[FetchedBoardState]]
        .withLoggingFinished { state => s"fetched sprint state: ${state.map(_.toString)}"  }
      updateResult <- fetchedCurrentSprintsBoardState.map { fetchedState =>
        projectActor ?? UpdateSprint(fetchedState.sprintId, fetchedState.userStories, finishSprint = false, timestamp)
      }.toFutureOfOption
    } yield updateResult
  }

  override val scrumSimulator: ScrumSimulator = new ScrumSimulator(boardStateProvider, projectActor)(initializationTimeout)
}
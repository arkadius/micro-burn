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

import net.liftweb.actor.{LAFuture, MockLiftActor}
import org.github.microburn.TestConfig
import org.github.microburn.domain._
import org.github.microburn.domain.actors.{GetProjectState, ProjectState, ProjectActorHelper, ProjectActor}
import org.scalatest._

class ScrumSimulatorTest extends FlatSpec with Matchers with ProjectActorHelper {

  import scala.concurrent.duration._
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._
  import OptionValues._

  implicit val config = ProjectConfig(TestConfig.withDefaultsFallback().getConfig("project"))

  it should "initial fetch project state" in {
    val givenSprint = SampleSprint.withEmptyEvents().copy(id = "1")
    val projectActor = projectActorWithInitialSprint(givenSprint)
    val boardStateProvider = new MockBoardStateProvider
    boardStateProvider.givenUserStories = Seq(SampleTasks.openedUserStory(123))
    val simulator = new ScrumSimulator(boardStateProvider, projectActor)(5.seconds)

    val state = (simulator ?? FetchCurrentSprintsBoardState).mapTo[Option[FetchedBoardState]].await(5.seconds)

    state.value shouldBe FetchedBoardState("1", givenSprint.details, boardStateProvider.givenUserStories)
  }

  it should "finish sprint" in {
    val givenId = "1"
    val givenSprint = SampleSprint.withEmptyEvents().copy(id = givenId)
    val projectActor = projectActorWithInitialSprint(givenSprint)
    val boardStateProvider = new MockBoardStateProvider
    val simulator = new ScrumSimulator(boardStateProvider, projectActor)(5.seconds)
    def sprintIsActive = projectHasOnlyOneSprint(givenId, projectActor)

    simulator ! FinishCurrentSprint

    (for {
      state <- (simulator ?? FetchCurrentSprintsBoardState).mapTo[Option[FetchedBoardState]]
      _ = {
        state shouldBe None
      }
      isActive <- sprintIsActive
    } yield {
      isActive shouldBe false
    }).await(5.seconds)
  }

  it should "create next sprint when none created before" in {
    val projectActor = projectActorWithoutSprints
    checkSprintStart(projectActor, expectedSprintId = "0")
  }

  it should "create next sprint when some created before" in {
    val givenId = "12"
    val givenSprint = SampleSprint.withEmptyEvents().copy(id = givenId)
    val projectActor = projectActorWithInitialSprint(givenSprint)
    checkSprintStart(projectActor, expectedSprintId = "13")
  }

  private def checkSprintStart(projectActor: ProjectActor, expectedSprintId: String): Unit = {
    val boardStateProvider = new MockBoardStateProvider
    val simulator = new ScrumSimulator(boardStateProvider, projectActor)(5.seconds)
    val givenName = "Foo Name"
    val givenStart = new Date(1000)
    val givenEnd = new Date(2000)

    val expectedDetails = SprintDetails(givenName, givenStart, givenEnd)
    def sprintIsActive = projectHasOnlyOneActiveSprint(expectedSprintId, projectActor)

    (for {
      _ <- simulator ?? StartSprint(givenName, givenStart, givenEnd)
      state <- (simulator ?? FetchCurrentSprintsBoardState).mapTo[Option[FetchedBoardState]]
      _ = {
        state.isDefined shouldBe true
        state.map(_.sprintId shouldEqual expectedSprintId)
        state.map(_.details shouldEqual expectedDetails)
      }
      foo <- sprintIsActive
    } yield foo).await(5.seconds)
  }

  class MockBoardStateProvider extends BoardStateProvider {
    var givenUserStories: Seq[UserStory] = Nil
    override def currentUserStories: LAFuture[Seq[UserStory]] = LAFuture(() => givenUserStories)
  }

}
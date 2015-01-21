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
package org.github.microburn.domain.actors

import java.io.File
import java.util.Date

import com.typesafe.config.ConfigFactory
import net.liftweb.common.Box
import org.github.microburn.{ConfigUtils, ApplicationContext}
import org.github.microburn.domain._
import org.github.microburn.repository.SprintRepository
import net.liftweb.actor.{LAFuture, MockLiftActor}
import org.github.microburn.service.SprintColumnsHistoryProvider
import org.github.microburn.util.concurrent.{LiftActorEnrichments, FutureEnrichments}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.io.Path
import scala.concurrent.duration._

class ProjectActorTest extends FlatSpec with Matchers {
  import FutureEnrichments._
  import LiftActorEnrichments._

  implicit val config = ProjectConfig(ConfigUtils.withToDefaultsFallback)

  it should "reply with correct active actors" in {
    val sprint = SampleSprint.withEmptyEvents()
    val projectActor = actorWithInitialSprint(sprint)
    def sprintIsActive = sprintActivenessCheck(sprint.id, projectActor)

   (for {
      beforeUpdateActiveness <- sprintIsActive
      _ = {
        beforeUpdateActiveness shouldBe true
        projectActor ! UpdateSprint(sprint.id, sprint.currentBoard.userStories, finishSprint = true, new Date)
      }
      afterUpdateActiveness <- sprintIsActive
    } yield {
      afterUpdateActiveness shouldBe false
    }).await(5 seconds)
  }

  it should "reply with correct history" in {
    val userStory = SampleTasks.openedUserStory(sp = 1)
    val sprint = SampleSprint.withEmptyEvents(userStory)
    val projectActor = actorWithInitialSprint(sprint)

    val userStoriesWithClosed = Seq(userStory.copy(status = ProjectConfigUtils.firstClosingStatus))
    val beforeSprintsEnd = new Date(sprint.details.end.getTime-1)
    projectActor ! UpdateSprint(sprint.id, userStoriesWithClosed, finishSprint = false, beforeSprintsEnd)

    val sprintHistoryBox = (projectActor ?? GetStoryPointsHistory(sprint.id)).mapTo[Box[SprintHistory]].await(5 seconds)
    val sprintHistory = sprintHistoryBox.openOrThrowException("")
    sprintHistory.initialStoryPointsSum shouldEqual 1
    val closedColumnHistory = sprintHistory.columnStates.map(_.storyPointsForColumn(ProjectConfigUtils.closingColumnIndex))
    closedColumnHistory shouldEqual Seq(0, 1)
  }

  private def actorWithInitialSprint(sprint: Sprint): ProjectActor = {
    val projectRoot = config.dataRoot
    Path(projectRoot).deleteRecursively()
    SprintRepository(new File(projectRoot, sprint.id), sprint.id).saveSprint(sprint)

    val projectActor = new ProjectActor(config)
    projectActor
  }

  private def sprintActivenessCheck(sprintId: String, projectActor: ProjectActor): LAFuture[Boolean] = {
    for {
      projectState <- (projectActor ?? GetProjectState).mapTo[ProjectState]
      sprintWithStates = projectState.sprints
    } yield {
      sprintWithStates should have length 1
      sprintWithStates.head.id shouldEqual sprintId
      sprintWithStates.head.isActive
    }
  }

}
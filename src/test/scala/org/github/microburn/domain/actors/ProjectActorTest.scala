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

import java.util.Date

import net.liftweb.actor.LAFuture
import net.liftweb.common.Box
import org.github.microburn.TestConfig
import org.github.microburn.domain._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class ProjectActorTest extends FlatSpec with Matchers with ProjectActorHelper {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._

  implicit val config = ProjectConfig(TestConfig.withDefaultsFallback().getConfig("project"))

  it should "reply with correct active actors finishing with UpdateSprint" in {
    finishActorCheck { sprint =>
      UpdateSprint(sprint.id, sprint.currentBoard.userStories, sprint.details.finish.openOrThrowException("").toMajor, new Date)
    }
  }

  it should "reply with correct active actors finishing with FinishSprint" in {
    finishActorCheck { sprint =>
      UpdateSprintDetails(sprint.id, sprint.details.finish.openOrThrowException(""), new Date)
    }
  }

  it should "reply with correct history" in {
    val userStory = SampleTasks.openedUserStory(sp = 1)
    val sprint = SampleSprint.withEmptyEvents(userStory)
    val projectActor = projectActorWithInitialSprint(sprint)

    val userStoriesWithClosed = Seq(userStory.copy(status = TaskCompletedStatus))
    val beforeSprintsEnd = new Date(sprint.details.end.getTime-1)
    projectActor ! UpdateSprint(sprint.id, userStoriesWithClosed, sprint.details.toMajor, beforeSprintsEnd)

    val sprintHistoryBox = (projectActor ?? GetStoryPointsHistory(sprint.id)).mapTo[Box[SprintHistory]].await(5.seconds)
    val sprintHistory = sprintHistoryBox.openOrThrowException("")
    sprintHistory.sprintBase.baseStoryPointsForColumnChanges shouldEqual 1
    val completedColumnHistory = sprintHistory.columnStates.map(_.storyPointsForColumn(config.lastDoneColumn.index))
    completedColumnHistory shouldEqual Seq(0, 1)
  }

  private def finishActorCheck(finishMethod: Sprint => Any): Unit = {
    val sprint = SampleSprint.withEmptyEvents()
    val projectActor = projectActorWithInitialSprint(sprint)
    def sprintIsActive = projectHasOnlyOneSprint(sprint.id, projectActor)

    (for {
      beforeUpdateActiveness <- sprintIsActive
      _ = {
        beforeUpdateActiveness shouldBe true
        projectActor ! finishMethod(sprint)
      }
      afterUpdateActiveness <- sprintIsActive
    } yield {
      afterUpdateActiveness shouldBe false
    }).await(5.seconds)
  }


}
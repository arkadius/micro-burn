package org.github.jiraburn.domain.actors

import java.io.File
import java.util.Date

import com.typesafe.config.ConfigFactory
import net.liftweb.common.Box
import org.github.jiraburn.ApplicationContext
import org.github.jiraburn.domain._
import org.github.jiraburn.repository.SprintRepository
import net.liftweb.actor.{LAFuture, MockLiftActor}
import org.github.jiraburn.service.SprintColumnsHistoryProvider
import org.github.jiraburn.util.concurrent.{LiftActorEnrichments, FutureEnrichments}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.io.Path
import scala.concurrent.duration._

class ProjectActorTest extends FlatSpec with Matchers {
  import FutureEnrichments._
  import LiftActorEnrichments._

  implicit val config = ProjectConfig(ConfigFactory.load())

  it should "reply with correct active actors" in {
    val sprint = FooSprint.withEmptyEvents()
    val projectActor = actorWithInitialSprint(sprint)
    def sprintIsActive = sprintActivenessCheck(sprint.id, projectActor)

   (for {
      beforeUpdateActiveness <- sprintIsActive
      _ = {
        beforeUpdateActiveness shouldBe true
        projectActor ! UpdateSprint(sprint.id, sprint.currentState.userStories, finishSprint = true, new Date)
      }
      afterUpdateActiveness <- sprintIsActive
    } yield {
      afterUpdateActiveness shouldBe false
    }).await(5 seconds)
  }

  it should "reply with correct history" in {
    val userStory = TaskGenerator.openedUserStory(sp = 1)
    val sprint = FooSprint.withEmptyEvents(userStory)
    val projectActor = actorWithInitialSprint(sprint)

    val userStoriesWithClosed = Seq(userStory.copy(status = config.firstClosingStatus))
    val beforeSprintsEnd = new Date(sprint.details.end.getTime-1)
    projectActor ! UpdateSprint(sprint.id, userStoriesWithClosed, finishSprint = false, beforeSprintsEnd)

    val sprintHistoryBox = (projectActor ?? GetStoryPointsHistory(sprint.id)).mapTo[Box[SprintHistory]].await(5 seconds)
    val sprintHistory = sprintHistoryBox.openOrThrowException("")
    sprintHistory.initialStoryPointsSum shouldEqual 1
    val closedColumnHistory = sprintHistory.columnStates.map(_.storyPointsForColumn(config.closingColumnIndex))
    closedColumnHistory shouldEqual Seq(0, 1)
  }

  private def actorWithInitialSprint(sprint: Sprint): ProjectActor = {
    val projectRoot = new File("target/projectActorTest")
    Path(projectRoot).deleteRecursively()
    SprintRepository(new File(projectRoot, sprint.id), sprint.id).saveSprint(sprint)

    val projectActor = new ProjectActor(projectRoot, config, new MockLiftActor)
    projectActor
  }

  private def sprintActivenessCheck(sprintId: String, projectActor: ProjectActor): LAFuture[Boolean] = {
    val sprintWithStatesFuture = (projectActor ?? GetSprintsWithStates).mapTo[Seq[SprintWithState]]
    sprintWithStatesFuture.map { sprintWithStates =>
      sprintWithStates should have length 1
      sprintWithStates.head.sprintId shouldEqual sprintId
      sprintWithStates.head.isActive
    }
  }
}
package com.example.actors

import java.io.File
import java.util.Date

import com.example.domain._
import com.example.repository.SprintRepository
import net.liftweb.actor.MockLiftActor
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.reflect.io.Path

class ProjectActorTest extends FlatSpec with Matchers {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  it should "reply with correct active actors" in {
    val sprint = FooSprint.withEmptyEvents(Nil)
    val projectActor = actorWithInitialSprint(sprint)
    def sprintIsActive = sprintActivenessCheck(sprint.id, projectActor)

    Await.result(for {
      beforeUpdateActiveness <- sprintIsActive
      _ = {
        beforeUpdateActiveness shouldBe true
        projectActor ! UpdateSprint(sprint.id, sprint.currentUserStories, finishSprint = true, new Date)
      }
      afterUpdateActiveness <- sprintIsActive
    } yield {
      afterUpdateActiveness shouldBe false
    }, 5 seconds)
  }

  it should "reply with correct history" in {
    val userStory = TaskGenerator.openedUserStory(sp = 1)
    val sprint = FooSprint.withEmptyEvents(Seq(userStory))
    val projectActor = actorWithInitialSprint(sprint)

    projectActor ! UpdateSprint(sprint.id, Seq(userStory.copy(state = Completed)), finishSprint = false, new Date)

    val historyFuture = (projectActor ?? (GetStoryPointsHistory(sprint.id), 5 seconds)).mapTo[StoryPointsHistory]

    Await.result(historyFuture.mapOrFail { history =>
      history.initialStoryPoints shouldEqual 1
      history.history.map(_.storyPoints) shouldEqual Seq(-1)
    }, 5 seconds)
  }

  private def actorWithInitialSprint(sprint: Sprint): ProjectActor = {
    val projectRoot = new File("target/projectActorTest")
    Path(projectRoot).deleteRecursively()
    SprintRepository(new File(projectRoot, sprint.id), sprint.id).saveSprint(sprint)(new Date)

    val projectActor = new ProjectActor(projectRoot, new MockLiftActor)
    projectActor
  }

  private def sprintActivenessCheck(sprintId: String, projectActor: ProjectActor): Future[Boolean] = {
    val sprintWithStatesFuture = (projectActor ?? (GetSprintsWithStates, timeout = 5 seconds)).mapTo[Seq[SprintWithState]]
    sprintWithStatesFuture.mapOrFail { sprintWithStates =>
      sprintWithStates should have length 1
      sprintWithStates.head.sprintId shouldEqual sprintId
      sprintWithStates.head.isActive
    }
  }

  implicit class FutureTestEnrichment[T](future: Future[T]) {
    def mapOrFail[TT](f: T => TT): Future[TT] = {
      future.onFailure {
        case ex => fail(ex)
      }
      future.map(f)
    }
  }
}

package org.github.jiraburn.repository

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.jiraburn.domain.{FooSprint, ProjectConfig, TaskGenerator}
import org.scalatest.{FlatSpec, Matchers}

class BoardStateRepositoryTest extends FlatSpec with Matchers {

  import org.scalatest.OptionValues._

  implicit val config = ProjectConfig(ConfigFactory.load())

  it should "work round trip" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(Some(2))
    val secTechnical = TaskGenerator.openedTechnicalTask(None)
    val userStories = Seq(TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical)))
    val sprint = FooSprint.withEmptyEvents(userStories : _*)
    val repo = BoardStateRepository(new File(s"target/sprints/${sprint.id}"))

    repo.saveCurrentUserStories(sprint)

    val loaded = repo.loadCurrentUserStories.value

    loaded shouldEqual sprint.currentBoard
  }

}
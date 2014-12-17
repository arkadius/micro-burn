package org.github.jiraburn.repository

import java.io.File
import java.util.Date

import com.typesafe.config.ConfigFactory
import org.github.jiraburn.domain.{ProjectConfig, FooSprint, TaskGenerator}
import org.scalatest.{FlatSpec, Matchers}

class SprintScopeRepositoryTest extends FlatSpec with Matchers {

  import org.scalatest.OptionValues._

  implicit val config = ProjectConfig(ConfigFactory.load())

  it should "work round trip" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(Some(2))
    val secTechnical = TaskGenerator.openedTechnicalTask(None)
    val userStories = Seq(TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical)))
    val sprint = FooSprint.withEmptyEvents(userStories)
    val repo = SprintScopeRepository(new File(s"target/sprints/${sprint.id}"))

    repo.saveCurrentUserStories(sprint)(new Date(1000))

    val loaded = repo.loadCurrentUserStories.value

    loaded shouldEqual userStories
  }

}
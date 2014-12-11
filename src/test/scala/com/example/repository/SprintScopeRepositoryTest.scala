package com.example.repository

import java.util.Date

import com.example.domain.{FooSprint, TaskGenerator}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

class SprintScopeRepositoryTest extends FlatSpec with Matchers {

  import org.scalatest.OptionValues._

  it should "work round trip" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(Some(2))
    val secTechnical = TaskGenerator.openedTechnicalTask(None)
    val userStories = Seq(TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical)))
    val sprint = FooSprint.withEmptyEvents(userStories)
    val repo = SprintScopeRepository(ConfigFactory.load())

    repo.saveUserStories(sprint)(new Date(1000))

    val loaded = repo.loadCurrentUserStories(sprint.id).value

    loaded shouldEqual userStories
  }

}

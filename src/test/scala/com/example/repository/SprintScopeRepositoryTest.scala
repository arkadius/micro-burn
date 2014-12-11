package com.example.repository

import com.example.domain.{FooSprint, Sprint, TaskGenerator}
import com.typesafe.config.{ConfigFactory, Config}
import org.scalatest.{Matchers, FlatSpec}

class SprintScopeRepositoryTest extends FlatSpec with Matchers {

  it should "work round trip" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(Some(2))
    val secTechnical = TaskGenerator.openedTechnicalTask(None)
    val userStories = Seq(TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical)))
    val sprint = FooSprint.withEmptyEvents(userStories)
    val repo = SprintScopeRepository(ConfigFactory.load())

    repo.saveUserStories(sprint)

    val loaded = repo.loadUserStories(sprint.id)

    loaded shouldEqual userStories
  }

}

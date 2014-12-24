package org.github.micoburn.repository

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.micoburn.ConfigUtils
import org.github.micoburn.domain.generator.SprintGenerator
import org.github.micoburn.domain.{SampleSprint, ProjectConfig, SampleTasks}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class BoardStateRepositoryRandomTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers {

  import org.scalatest.OptionValues._

  implicit val config = ProjectConfig(ConfigUtils.withToDefaultsFallback)

  it should "work round trip" in {
    forAll(SprintGenerator.withEmptyEvents) { sprint =>
      val repo = BoardStateRepository(new File(s"target/sprints/${sprint.id}"))

      repo.saveCurrentUserStories(sprint)

      val loaded = repo.loadCurrentUserStories.value

      loaded shouldEqual sprint.currentBoard
    }
  }

}
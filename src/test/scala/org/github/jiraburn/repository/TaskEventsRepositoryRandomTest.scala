package org.github.jiraburn.repository

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.jiraburn.domain.ProjectConfig
import org.github.jiraburn.domain.generator.TaskEventsGenerator
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class TaskEventsRepositoryRandomTest extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  implicit val config = ProjectConfig(ConfigFactory.load())

  it should "do correct round trip" in {
    forAll(Gen.nonEmptyListOf(TaskEventsGenerator.generator)) { events =>
      val sprintRoot = new File(s"target/sprints/foo")
      val file = new File(sprintRoot, "taskEvents.csv")
      if (file.exists())
        file.delete()
      val repo = TaskEventsRepository(sprintRoot)

      repo.appendTasksEvents(events)

      val loaded = repo.loadTaskEvents
      loaded.size shouldEqual events.size
      loaded.head shouldEqual events.head
      loaded shouldEqual events
    }
  }
}

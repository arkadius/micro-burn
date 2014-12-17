package org.github.jiraburn.repository

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.jiraburn.domain.{TaskChanged, ProjectConfig, TaskEventsGenerator}
import org.scalatest.{FlatSpec, Matchers}

class TaskEventsRepositoryTest extends FlatSpec with Matchers {

  implicit val config = ProjectConfig(ConfigFactory.load())

  it should "do correct round trip" in {
    val sprintRoot = new File(s"target/sprints/foo")
    val file = new File(sprintRoot, "taskEvents.csv")
    if (file.exists())
      file.delete()

    val events = Seq(
      TaskEventsGenerator.completedEvent,
      TaskEventsGenerator.completedEvent,
      TaskEventsGenerator.reopenedEvent
    )
    val repo = TaskEventsRepository(sprintRoot)

    repo.appendTasksEvents(events)

    val loaded = repo.loadTaskEvents
    loaded.size shouldEqual events.size
    loaded.head shouldEqual events.head
    loaded shouldEqual events
  }

}

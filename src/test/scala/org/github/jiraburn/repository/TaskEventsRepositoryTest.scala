package org.github.jiraburn.repository

import java.io.File

import org.github.jiraburn.domain.{TaskEvent, TaskEventsGenerator}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec}

class TaskEventsRepositoryTest extends FlatSpec with Matchers {

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

    repo.loadTaskEvents shouldEqual events
  }

}

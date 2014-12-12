package com.example.repository

import java.io.File

import com.example.domain.{TaskEvent, TaskEventsGenerator}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec}

class TaskEventsRepositoryTest extends FlatSpec with Matchers {

  it should "do correct round trip" in {
    val config = ConfigFactory.load()
    val file = new File(config.getString("data.events"))
    if (file.exists())
      file.delete()

    val events = Seq(
      TaskEventsGenerator.completedEvent,
      TaskEventsGenerator.completedEvent,
      TaskEventsGenerator.reopenedEvent
    )
    val repo = TaskEventsRepository(config)

    repo.appendTasksEvents(events)

    repo.loadTaskEvents shouldEqual events
  }

}

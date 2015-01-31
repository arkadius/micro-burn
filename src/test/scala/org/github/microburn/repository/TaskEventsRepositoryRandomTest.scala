/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.repository

import java.io.File

import org.github.microburn.TestConfig
import org.github.microburn.domain.ProjectConfig
import org.github.microburn.domain.generator.TaskEventsGenerator
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class TaskEventsRepositoryRandomTest extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  implicit val config = ProjectConfig(TestConfig.withDefaultsFallback())

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
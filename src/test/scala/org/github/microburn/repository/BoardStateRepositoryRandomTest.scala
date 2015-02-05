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

import com.typesafe.config.ConfigFactory
import org.github.microburn.TestConfig
import org.github.microburn.domain.generator.SprintGenerator
import org.github.microburn.domain.{ProjectConfigUtils, SampleSprint, ProjectConfig, SampleTasks}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class BoardStateRepositoryRandomTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers {

  import org.scalatest.OptionValues._

  implicit val config = ProjectConfigUtils.defaultConfig

  it should "work round trip" in {
    forAll(SprintGenerator.withEmptyEvents) { sprint =>
      val repo = BoardStateRepository(new File(s"target/sprints/${sprint.id}"))

      repo.saveCurrentUserStories(sprint)

      val loaded = repo.loadCurrentUserStories.value

      loaded shouldEqual sprint.currentBoard
    }
  }

}
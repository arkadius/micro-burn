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

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

class LastSprintRestartRepositoryTest extends FlatSpec with Matchers {

  import org.scalatest.OptionValues._

  it should "do round-trip" in {
    val dir = new File("target/sprints")
    val file = new File(dir, "lastSprintRestart.txt")
    if (file.exists())
      file.delete()
    val repo = LastSprintRestartRepository(dir)

    repo.loadLastSprintRestart.isEmpty shouldBe true

    val saved = new DateTime(2015, 1, 1, 12, 11)
    repo.saveLastSprintRestart(saved)

    repo.loadLastSprintRestart.value shouldEqual saved
  }

}
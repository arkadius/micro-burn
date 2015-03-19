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
package org.github.microburn.domain

import org.github.microburn.TestConfig
import org.scalatest.{FlatSpec, Matchers}

class ProjectConfigTest extends FlatSpec with Matchers {

  it should "create project config from conf having columns with statuses" in {
    val config = ProjectConfigUtils.defaultConfig

    config.nonBacklogColumns should have length 5
  }

  it should "create project config from conf having columns with ids" in {
    val config = ProjectConfig(TestConfig.trelloConfigWithDefaultsFallback().getConfig("project"))

    config.nonBacklogColumns should have length 2
    config.nonBacklogColumns(0).isDoneColumn shouldBe false
    config.nonBacklogColumns(1).isDoneColumn shouldBe true
  }

}
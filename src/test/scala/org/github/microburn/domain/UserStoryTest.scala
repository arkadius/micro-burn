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

import org.scalatest.{Matchers, FlatSpec, FunSuite}

class UserStoryTest extends FlatSpec with Matchers {

  it should "not split sp by default" in {
    implicit val config = ProjectConfigUtils.defaultConfig
    val technical = SampleTasks.openedTechnicalTask(None)
    val story = SampleTasks.openedUserStory(3, Seq(technical))

    story.storyPointsWithoutSubTasks shouldEqual BigDecimal(3)
  }

  it should "split sp if split flag is on" in {
    implicit val config = ProjectConfigUtils.defaultConfig.copy(splitSpBetweenTechnicalTasks = true)
    val technicalWithSp = SampleTasks.openedTechnicalTask(Some(BigDecimal("1.9")))
    val technicalWithoutSp1 = SampleTasks.openedTechnicalTask(None)
    val technicalWithoutSp2 = SampleTasks.openedTechnicalTask(None)
    val story = SampleTasks.openedUserStory(3, Seq(technicalWithSp, technicalWithoutSp1, technicalWithoutSp2))

    story.storyPointsWithoutSubTasks shouldEqual BigDecimal("0.1")
    story.flattenTasks.find(_.taskId == technicalWithSp.taskId).get.storyPointsWithoutSubTasks shouldEqual BigDecimal("1.9")
    story.flattenTasks.find(_.taskId == technicalWithoutSp1.taskId).get.storyPointsWithoutSubTasks shouldEqual BigDecimal("0.5")
    story.flattenTasks.find(_.taskId == technicalWithoutSp2.taskId).get.storyPointsWithoutSubTasks shouldEqual BigDecimal("0.5")
  }
}
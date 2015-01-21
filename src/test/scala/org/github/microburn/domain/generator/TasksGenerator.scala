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
package org.github.microburn.domain.generator

import org.github.microburn.domain.{TechnicalTask, UserStory}
import org.scalacheck.Gen

object TasksGenerator {

  import scala.collection.convert.wrapAsScala._

  def technicalTaskGenerator(optionalStoryPoints: Option[Int]): Gen[TechnicalTask] = {
    for {
      taskId <- Gen.uuid
      taskName <- Gen.identifier
      status <- Gen.posNum[Int]
    } yield TechnicalTask(taskId = taskId.toString, taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status)
  }

  private val technicalTasksAndStoryPointsGenerator: Gen[(List[TechnicalTask], Option[Int])] = {
    for {
      technicalTasksCount <- Gen.posNum[Int]
      optionalStoryPointsParts <- Gen.option(Gen.listOfN(technicalTasksCount , Gen.posNum[Int]))
      userStorySelfStoryPoints <- Gen.posNum[Int]
      sum = optionalStoryPointsParts.map { parts =>
        parts.sum + userStorySelfStoryPoints
      }
      technicalTaskStoryPoints = optionalStoryPointsParts.map { parts =>
        parts.map(Some(_))
      } getOrElse (0 until technicalTasksCount).map(_ => None)
      technicalTasks <- Gen.sequence(technicalTaskStoryPoints.map(technicalTaskGenerator)).map(_.toList)
    } yield (technicalTasks, sum)
  }

  val userStoryGenerator: Gen[UserStory] = {
    for {
      taskId <- Gen.uuid
      taskName <- Gen.identifier
      (technicalTasks, optionalStoryPoints) <- technicalTasksAndStoryPointsGenerator
      status <- Gen.posNum[Int]
    } yield UserStory(
      taskId = taskId.toString, taskName = taskName, optionalStoryPoints = optionalStoryPoints,
      technicalTasksWithoutParentId = technicalTasks.toIndexedSeq, status = status
    )
  }

}
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

import java.util.Date

import org.github.microburn.domain.{TaskAdded, TaskEvent, TaskRemoved, TaskUpdated}
import org.scalacheck.Gen

object TaskEventsGenerator {

  private val addedEventGenerator: Gen[TaskAdded] = {
    for {
      taskId <- Gen.identifier
      parentUserStoryId <- Gen.identifier
      isTechnicalTask <- Gen.oneOf(true, false)
      taskName <- Gen.identifier
      optionalStoryPoints <- Gen.option(Gen.posNum[Int])
      status <- Gen.posNum[Int]
      date <- Gen.posNum[Long].map(new Date(_))
    } yield TaskAdded(
      taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask,
      taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status.toString, date = date
    )
  }

  private val updatedEventGenerator: Gen[TaskUpdated] = {
    for {
      taskId <- Gen.identifier
      parentUserStoryId <- Gen.identifier
      isTechnicalTask <- Gen.oneOf(true, false)
      taskName <- Gen.identifier
      optionalStoryPoints <- Gen.option(Gen.posNum[Int])
      status <- Gen.posNum[Int]
      date <- Gen.posNum[Long].map(new Date(_))
    } yield TaskUpdated(
      taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask,
      taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status.toString, date = date
    )
  }

  private val removedEventGenerator: Gen[TaskRemoved] = {
    for {
      taskId <- Gen.identifier
      parentUserStoryId <- Gen.identifier
      isTechnicalTask <- Gen.oneOf(true, false)
      date <- Gen.posNum[Long].map(new Date(_))
    } yield TaskRemoved(
      taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask, date = date
    )
  }

  val generator: Gen[TaskEvent] =
    Gen.oneOf(addedEventGenerator, updatedEventGenerator, removedEventGenerator)

}
package org.github.micoburn.domain.generator

import java.util.Date

import org.github.micoburn.domain.{TaskAdded, TaskEvent, TaskRemoved, TaskUpdated}
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
      taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status, date = date
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
      taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status, date = date
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
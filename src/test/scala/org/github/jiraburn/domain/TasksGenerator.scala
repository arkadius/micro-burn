package org.github.jiraburn.domain

import org.scalacheck.Gen

object TasksGenerator {

  import collection.convert.wrapAsScala._

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
      technicalTasksWithoutParentId = technicalTasks.toSet, status = status
    )
  }

}

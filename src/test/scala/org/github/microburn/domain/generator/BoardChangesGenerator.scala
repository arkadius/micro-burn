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

import org.github.microburn.domain._
import org.scalacheck.Gen

object BoardChangesGenerator {

  private def updateStatus(technicalTask: TechnicalTask): Gen[TechnicalTask] =
    for {
      status <- TaskStatusGenerator.generator
    } yield technicalTask.copy(status = status)

  private def updateStoryPoints(technicalTask: TechnicalTask): Gen[TechnicalTask] =
    for {
      optionalStoryPoints <- StoryPointsGenerator.optionalSpGenerator
    } yield technicalTask.copy(optionalStoryPoints = optionalStoryPoints)

  private def updateTechnicalTaskGenerator(board: BoardState): Gen[BoardState] =
    for {
      parent <- Gen.oneOf(board.userStories.toSeq)
      technical <- Gen.oneOf(parent.technicalTasksWithoutParentId.toSeq)
      updatedTechnical <- Gen.oneOf(updateStatus(technical), updateStoryPoints(technical))
      updated = parent.update(technical.taskId)(_ => updatedTechnical)
    } yield board.withUpdateNestedTask(updated)

  private def removeTechnicalTaskGenerator(board: BoardState): Gen[BoardState] =
    for {
      parent <- Gen.oneOf(board.userStories.toSeq)
      technicalTaskId <- Gen.oneOf(parent.technicalTasksWithoutParentId.toSeq.map(_.taskId))
      updated = parent.remove(technicalTaskId)      
    } yield board.withUpdateNestedTask(updated)
  
  private def addTechnicalTaskGenerator(board: BoardState)(implicit config: ProjectConfig): Gen[BoardState] =
    for {
      parent <- Gen.oneOf(board.userStories.toSeq)
      technicalStoryPoints <- StoryPointsGenerator.chooseAtMost(parent.storyPointsWithoutSubTasks)
      technical <- TasksGenerator.technicalTaskGenerator(Some(technicalStoryPoints))
      updated = parent.add(technical)
    } yield board.withUpdateNestedTask(updated)

  private def updateStatus(userStory: UserStory): Gen[UserStory] =
    for {
      status <- TaskStatusGenerator.generator
    } yield userStory.copy(status = status)
  
  private def updateStoryPoints(userStory: UserStory): Gen[UserStory] = 
    for {
      optionalStoryPoints <- StoryPointsGenerator.optionalSpGenerator
    } yield userStory.copy(optionalStoryPoints = optionalStoryPoints)
  
  private def updateUserStoryGenerator(board: BoardState): Gen[BoardState] =
    for {
      task <- Gen.oneOf(board.userStories.toSeq)
      updated <- Gen.oneOf(updateStatus(task), updateStoryPoints(task))
    } yield board.withUpdateNestedTask(updated)

  private def removeUserStoryGenerator(board: BoardState): Gen[BoardState] =
    for {
      taskId <- Gen.oneOf(board.userStories.map(_.taskId).toSeq)
    } yield board.copy(userStories = board.userStories.filterNot(_.taskId == taskId))

  private def addUserStoryGenerator(board: BoardState): Gen[BoardState] =
    for {
      userStory <- TasksGenerator.userStoryGenerator
    } yield board.copy(userStories = board.userStories :+ userStory)
  
  def changesGenerator(board: BoardState)(implicit config: ProjectConfig): Gen[BoardState] =
    Gen.oneOf(
      updateTechnicalTaskGenerator(board),
      removeTechnicalTaskGenerator(board),
      addTechnicalTaskGenerator(board),
      updateUserStoryGenerator(board),
      removeUserStoryGenerator(board),
      addUserStoryGenerator(board)
    )
}
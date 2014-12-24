package org.github.jiraburn.domain

import org.scalacheck.Gen

object BoardChangesGenerator {

  private def updateStatus(technicalTask: TechnicalTask): Gen[TechnicalTask] =
    for {
      status <- Gen.posNum[Int]
    } yield technicalTask.copy(status = status)

  private def updateStoryPoints(technicalTask: TechnicalTask): Gen[TechnicalTask] =
    for {
      optionalStoryPoints <- Gen.option(Gen.posNum[Int])
    } yield technicalTask.copy(optionalStoryPoints = optionalStoryPoints)

  private def updateTechnicalTaskGenerator(board: BoardState): Gen[BoardState] =
    for {
      parent <- Gen.oneOf(board.userStories.toSeq)
      technical <- Gen.oneOf(parent.technicalTasksWithoutParentId.toSeq)
      updatedTechnical <- Gen.oneOf(updateStatus(technical), updateStoryPoints(technical))
      updated = parent.update(technical.taskId)(_ => updatedTechnical)
    } yield board.copy(userStories = board.userStories.filterNot(_.taskId == parent.taskId) + updated)

  private def removeTechnicalTaskGenerator(board: BoardState): Gen[BoardState] =
    for {
      parent <- Gen.oneOf(board.userStories.toSeq)
      technicalTaskId <- Gen.oneOf(parent.technicalTasksWithoutParentId.toSeq.map(_.taskId))
      updated = parent.remove(technicalTaskId)      
    } yield board.copy(userStories = board.userStories.filterNot(_.taskId == parent.taskId) + updated)
  
  private def addTechnicalTaskGenerator(board: BoardState): Gen[BoardState] =
    for {
      parent <- Gen.oneOf(board.userStories.toSeq)
      technicalStoryPoints <- Gen.chooseNum(0, parent.storyPointsWithoutSubTasks)
      technical <- TasksGenerator.technicalTaskGenerator(Some(technicalStoryPoints))
      updated = parent.add(technical)
    } yield board.copy(userStories = board.userStories.filterNot(_.taskId == parent.taskId) + updated)

  private def updateStatus(userStory: UserStory): Gen[UserStory] =
    for {
      status <- Gen.posNum[Int]
    } yield userStory.copy(status = status) 
  
  private def updateStoryPoints(userStory: UserStory): Gen[UserStory] = 
    for {
      optionalStoryPoints <- Gen.option(Gen.posNum[Int])
    } yield userStory.copy(optionalStoryPoints = optionalStoryPoints)
  
  private def updateUserStoryGenerator(board: BoardState): Gen[BoardState] =
    for {
      task <- Gen.oneOf(board.userStories.toSeq)
      updated <- Gen.oneOf(updateStatus(task), updateStoryPoints(task))
    } yield board.copy(userStories = board.userStories.filterNot(_.taskId == task.taskId) + updated)

  private def removeUserStoryGenerator(board: BoardState): Gen[BoardState] =
    for {
      taskId <- Gen.oneOf(board.userStories.map(_.taskId).toSeq)
    } yield board.copy(userStories = board.userStories.filterNot(_.taskId == taskId))

  private def addUserStoryGenerator(board: BoardState): Gen[BoardState] =
    for {
      userStory <- TasksGenerator.userStoryGenerator
    } yield board.copy(userStories = board.userStories + userStory)
  
  def changesGenerator(board: BoardState): Gen[BoardState] =
    Gen.oneOf(
//      updateTechnicalTaskGenerator(board),
//      removeTechnicalTaskGenerator(board),
//      addTechnicalTaskGenerator(board),
//      updateUserStoryGenerator(board),
      removeUserStoryGenerator(board),
      removeUserStoryGenerator(board)
//      addUserStoryGenerator(board)
    )  
}

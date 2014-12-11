package com.example.domain

import java.util.Date

import org.scalatest.{Matchers, FlatSpec}

class SprintTest extends FlatSpec with Matchers {

  it should "give correct story points sum" in {
    val sprint = Sprint.withEmptyEvents(Seq(
      TaskGenerator.openedUserStory(1),
      TaskGenerator.completedUserStory(2)
    ))

    sprint.summedInitialStoryPoints shouldBe 3
  }

  it should "produce correct events for update" in {
    val taskInitiallyOpened = TaskGenerator.openedUserStory(1)
    val taskInitiallyCompleted = TaskGenerator.completedUserStory(2)

    val sprintBeforeUpdate = Sprint.withEmptyEvents(Seq(taskInitiallyOpened, taskInitiallyCompleted))

    val firstTaskAfterFinish = taskInitiallyOpened.copy(state = Completed)
    val sprintAfterFirstFinish = sprintBeforeUpdate.userStoriesUpdated(Seq(firstTaskAfterFinish, taskInitiallyCompleted))(new Date(100))
    sprintAfterFirstFinish.summedInitialStoryPoints shouldBe 3
    sprintAfterFirstFinish.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1)

    val secTaskAfterReopen = taskInitiallyCompleted.copy(state = Opened)
    val sprintAfterSecReopen = sprintAfterFirstFinish.userStoriesUpdated(Seq(firstTaskAfterFinish, secTaskAfterReopen))(new Date(200))
    sprintAfterSecReopen.summedInitialStoryPoints shouldBe 3
    sprintAfterSecReopen.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1, 1)
  }

  it should "generate empty events for not estimated technical tasks and non empty for parent user stories" in {
    val technical = TaskGenerator.openedTechnicalTask(optionalSP = None)
    val userStory = TaskGenerator.openedUserStory(1, Seq(technical))
    val sprint = Sprint.withEmptyEvents(Seq(userStory))

    val completedTechnical = technical.copy(state = Completed)
    val completedUserStory = userStory.copy(state = Completed, technicalTasks = Seq(completedTechnical))

    val afterUpdate = sprint.userStoriesUpdated(Seq(completedUserStory))(new Date)

    afterUpdate.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1)
  }

  it should "generate non empty events for estimated technical tasks and empty for parent user stories" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(2))
    val userStory = TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = Sprint.withEmptyEvents(Seq(userStory))

    val completedFirstTechnical = firstTechnical.copy(state = Completed)
    val completedFirstUserStory = userStory.copy(technicalTasks = Seq(completedFirstTechnical, secTechnical))
    val afterFirstFinish = sprint.userStoriesUpdated(Seq(completedFirstUserStory))(new Date(100))
    afterFirstFinish.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1)

    val completedSecTechnical = secTechnical.copy(state = Completed)
    val completedAllUserStory = completedFirstUserStory.copy(state = Completed, technicalTasks = Seq(completedFirstTechnical, completedSecTechnical))
    val afterAllFinish = afterFirstFinish.userStoriesUpdated(Seq(completedAllUserStory))(new Date(200))
    afterAllFinish.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1, -3)
  }
}

object TaskGenerator {

  private var currentId = 0

  private def nextId = {
    currentId += 1
    currentId.toString
  }

  def openedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil) =
    UserStory(nextId, Some(sp), technical, Opened)

  def completedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil) =
    UserStory(nextId, Some(sp), technical, Completed)


  def openedTechnicalTask(optionalSP: Option[Int]) = TechnicalTask(nextId, optionalSP, Opened)
  
  def completedTechnicalTask(optionalSP: Option[Int]) = TechnicalTask(nextId, optionalSP, Completed)

}
package org.github.jiraburn.domain

import java.util.Date

import org.scalatest.{Matchers, FlatSpec}

class SprintTest extends FlatSpec with Matchers {

  it should "give correct story points sum" in {
    val sprint = FooSprint.withEmptyEvents(
      Seq(
        TaskGenerator.openedUserStory(1),
        TaskGenerator.completedUserStory(2)
      )
    )

    sprint.initialStoryPoints shouldBe 3
  }

  it should "produce correct events for update" in {
    val taskInitiallyOpened = TaskGenerator.openedUserStory(1)
    val taskInitiallyCompleted = TaskGenerator.completedUserStory(2)

    val sprintBeforeUpdate = FooSprint.withEmptyEvents(Seq(taskInitiallyOpened, taskInitiallyCompleted))

    val firstTaskAfterFinish = taskInitiallyOpened.copy(state = Completed)
    val sprintAfterFirstFinish = sprintBeforeUpdate.update(Seq(firstTaskAfterFinish, taskInitiallyCompleted), finishSprint = false)(new Date(100)).updatedSprint
    sprintAfterFirstFinish.initialStoryPoints shouldBe 3
    sprintAfterFirstFinish.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1)

    val secTaskAfterReopen = taskInitiallyCompleted.copy(state = Opened)
    val sprintAfterSecReopen = sprintAfterFirstFinish.update(Seq(firstTaskAfterFinish, secTaskAfterReopen), finishSprint = false)(new Date(200)).updatedSprint
    sprintAfterSecReopen.initialStoryPoints shouldBe 3
    sprintAfterSecReopen.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1, 1)
  }

  it should "generate empty events for not estimated technical tasks and non empty for parent user stories" in {
    val technical = TaskGenerator.openedTechnicalTask(optionalSP = None)
    val userStory = TaskGenerator.openedUserStory(1, Seq(technical))
    val sprint = FooSprint.withEmptyEvents(Seq(userStory))

    val completedTechnical = technical.copy(state = Completed)
    val completedUserStory = userStory.copy(state = Completed, technicalTasksWithoutParentId = List(completedTechnical))

    val afterUpdate = sprint.update(Seq(completedUserStory), finishSprint = false)(new Date).updatedSprint

    afterUpdate.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1)
  }

  it should "generate non empty events for estimated technical tasks and empty for parent user stories" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(2))
    val userStory = TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = FooSprint.withEmptyEvents(Seq(userStory))

    val completedFirstTechnical = firstTechnical.copy(state = Completed)
    val completedFirstUserStory = userStory.copy(technicalTasksWithoutParentId = List(completedFirstTechnical, secTechnical))
    val afterFirstFinish = sprint.update(Seq(completedFirstUserStory), finishSprint = false)(new Date(100)).updatedSprint
    afterFirstFinish.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1)

    val completedSecTechnical = secTechnical.copy(state = Completed)
    val completedAllUserStory = completedFirstUserStory.copy(state = Completed, technicalTasksWithoutParentId = List(completedFirstTechnical, completedSecTechnical))
    val afterAllFinish = afterFirstFinish.update(Seq(completedAllUserStory), finishSprint = false)(new Date(200)).updatedSprint
    afterAllFinish.storyPointsChanges.map(_.storyPoints) shouldEqual Seq(-1, -3)
  }
}


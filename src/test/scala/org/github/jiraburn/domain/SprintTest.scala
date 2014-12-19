package org.github.jiraburn.domain

import java.util.Date

import com.typesafe.config.ConfigFactory
import org.scalatest.{Inside, FlatSpec, Matchers}

class SprintTest extends FlatSpec with Matchers with Inside {

  implicit val config = ProjectConfig(ConfigFactory.load())

  it should "give correct story points sum" in {
    val sprint = FooSprint.withEmptyEvents(
      TaskGenerator.openedUserStory(1),
      TaskGenerator.closedUserStory(2)
    )

    sprint.initialStoryPoints shouldBe 3
  }

  it should "produce correct events for update" in {
    val taskInitiallyOpened = TaskGenerator.openedUserStory(1)
    val taskInitiallyCompleted = TaskGenerator.closedUserStory(2)

    val sprintBeforeUpdate = FooSprint.withEmptyEvents(taskInitiallyOpened, taskInitiallyCompleted)

    val sprintAfterFirstFinish = sprintBeforeUpdate.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted)
    sprintAfterFirstFinish.initialStoryPoints shouldBe 3
    sprintAfterFirstFinish.storyPointsChangesValues shouldEqual Seq(-1)

    val sprintAfterSecReopen = sprintAfterFirstFinish.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted.reopen)
    sprintAfterSecReopen.initialStoryPoints shouldBe 3
    sprintAfterSecReopen.storyPointsChangesValues shouldEqual Seq(-1, 1)
  }

  it should "generate empty events for not estimated technical tasks and non empty for parent user stories" in {
    val technical = TaskGenerator.openedTechnicalTask(optionalSP = None)
    val userStory = TaskGenerator.openedUserStory(1, Seq(technical))
    val sprint = FooSprint.withEmptyEvents(userStory)

    val completedUserStory = userStory.copy(technicalTasksWithoutParentId = List(technical.close)).close

    val afterUpdate = sprint.updateTasks(completedUserStory)

    afterUpdate.storyPointsChangesValues shouldEqual Seq(-1)
  }

  it should "generate non empty events for estimated technical tasks and empty for parent user stories" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(1))
    val userStory = TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = FooSprint.withEmptyEvents(userStory)

    val completedFirstUserStory = userStory.copy(technicalTasksWithoutParentId = List(firstTechnical.close, secTechnical))
    val afterFirstFinish = sprint.updateTasks(completedFirstUserStory)
    afterFirstFinish.storyPointsChangesValues shouldEqual Seq(-1)

    val completedAllUserStory = completedFirstUserStory.copy(technicalTasksWithoutParentId = List(firstTechnical.close, secTechnical.close)).close
    val afterAllFinish = afterFirstFinish.updateTasks(completedAllUserStory)
    afterAllFinish.storyPointsChangesValues shouldEqual Seq(-1, -3)
  }

  it should "generate correct events for scope change" in {
    val firstTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = TaskGenerator.openedTechnicalTask(optionalSP = Some(1))
    val userStory = TaskGenerator.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = FooSprint.withEmptyEvents(userStory)

    val secTechnicalClosed = secTechnical.close
    val withSecClosed = userStory.copy(technicalTasksWithoutParentId = List(firstTechnical, secTechnicalClosed))
    val afterSecClose = sprint.updateTasks(withSecClosed)
    afterSecClose.storyPointsChangesValues shouldEqual Seq(-1)

    val secTechnicalWithChangedScope = secTechnicalClosed.copy(optionalStoryPoints = Some(2))
    val changedScope = withSecClosed.copy(technicalTasksWithoutParentId = List(firstTechnical, secTechnicalWithChangedScope))
    val afterScopeChange = afterSecClose.updateTasks(changedScope)
    inside(afterScopeChange) {
      case _ => afterScopeChange.storyPointsChangesValues shouldEqual Seq(-1, -3)
      // FIXME: przez to, że analizując techniczny nie patrzymy na zmiany w parencie, jest podwójny scope change
    }

    val completedAllUserStory = changedScope.copy(technicalTasksWithoutParentId = List(firstTechnical.close, secTechnicalWithChangedScope)).close
    val afterAllFinish = afterScopeChange.updateTasks(completedAllUserStory)
    afterAllFinish.storyPointsChangesValues shouldEqual Seq(-1, -2, -4) // FIXME: jw
  }

  // TODO: test na pojawianie się / znikanie tasków technicznych

  private val dateIterator = Stream.from(100, 100).map { i => new Date(i.toLong) }.toIterable.iterator

  private def nextDate = dateIterator.next()

  implicit class EnhancedSprint(sprint: Sprint) {
    def updateTasks(updatedTasks: UserStory*) = sprint.update(updatedTasks, finishSprint = false)(nextDate).updatedSprint

    def storyPointsChangesValues = sprint.storyPointsChanges.map(_.storyPoints)
  }
  
  implicit class EnhancedUserStory(userStory: UserStory) {
    def close = userStory.copy(status = config.firstClosingStatus)
    def reopen = userStory.copy(status = config.firstNotClosingStatus)
  }

  implicit class EnhancedTechnicalTask(technicalTask: TechnicalTask) {
    def close = technicalTask.copy(status = config.firstClosingStatus)
    def reopen = technicalTask.copy(status = config.firstNotClosingStatus)
  }
}


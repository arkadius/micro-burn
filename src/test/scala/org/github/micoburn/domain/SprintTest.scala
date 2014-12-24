package org.github.micoburn.domain

import java.util.Date

import com.typesafe.config.ConfigFactory
import org.github.micoburn.{ConfigUtils, ApplicationContext}
import org.scalatest.{Inside, FlatSpec, Matchers}

class SprintTest extends FlatSpec with Matchers with Inside {

  implicit val config = ProjectConfig(ConfigUtils.withToDefaultsFallback)

  it should "give correct story points sum" in {
    val sprint = SampleSprint.withEmptyEvents(
      SampleTasks.openedUserStory(1),
      SampleTasks.closedUserStory(2)
    )

    sprint.initialStoryPointsSum shouldBe 3
  }

  it should "produce correct events for update" in {
    val taskInitiallyOpened = SampleTasks.openedUserStory(1)
    val taskInitiallyCompleted = SampleTasks.closedUserStory(2)

    val sprintBeforeUpdate = SampleSprint.withEmptyEvents(taskInitiallyOpened, taskInitiallyCompleted)

    val sprintAfterFirstFinish = sprintBeforeUpdate.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted)
    sprintAfterFirstFinish.initialStoryPointsSum shouldBe 3
    sprintAfterFirstFinish.storyPointsChangesValues shouldEqual Seq(2, 3)

    val sprintAfterSecReopen = sprintAfterFirstFinish.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted.reopen)
    sprintAfterSecReopen.initialStoryPointsSum shouldBe 3
    sprintAfterSecReopen.storyPointsChangesValues shouldEqual Seq(2, 3, 1)
  }

  it should "generate empty events for not estimated technical tasks and non empty for parent user stories" in {
    val technical = SampleTasks.openedTechnicalTask(optionalSP = None)
    val userStory = SampleTasks.openedUserStory(1, Seq(technical))
    val sprint = SampleSprint.withEmptyEvents(userStory)

    val completedUserStory = userStory.copy(technicalTasksWithoutParentId = IndexedSeq(technical.close)).close

    val afterUpdate = sprint.updateTasks(completedUserStory)

    afterUpdate.storyPointsChangesValues shouldEqual Seq(0, 1)
  }

  it should "generate non empty events for estimated technical tasks and empty for parent user stories" in {
    val firstTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val userStory = SampleTasks.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = SampleSprint.withEmptyEvents(userStory)

    val completedFirstUserStory = userStory.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical.close, secTechnical))
    val afterFirstFinish = sprint.updateTasks(completedFirstUserStory)
    afterFirstFinish.storyPointsChangesValues shouldEqual Seq(0, 1)

    val completedAllUserStory = completedFirstUserStory.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical.close, secTechnical.close)).close
    val afterAllFinish = afterFirstFinish.updateTasks(completedAllUserStory)
    afterAllFinish.storyPointsChangesValues shouldEqual Seq(0, 1, 3)
  }

  it should "generate correct events for scope change" in {
    val firstTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val userStory = SampleTasks.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = SampleSprint.withEmptyEvents(userStory)

    val secTechnicalClosed = secTechnical.close
    val withSecClosed = userStory.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical, secTechnicalClosed))
    val afterSecClose = sprint.updateTasks(withSecClosed)
    afterSecClose.storyPointsChangesValues shouldEqual Seq(0, 1)

    val secTechnicalWithChangedScope = secTechnicalClosed.copy(optionalStoryPoints = Some(2))
    val changedScope = withSecClosed.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical, secTechnicalWithChangedScope))
    val afterScopeChange = afterSecClose.updateTasks(changedScope)
    inside(afterScopeChange) {
      case _ => afterScopeChange.storyPointsChangesValues shouldEqual Seq(0, 1, 2)
    }

    val completedAllUserStory = changedScope.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical.close, secTechnicalWithChangedScope)).close
    val afterAllFinish = afterScopeChange.updateTasks(completedAllUserStory)
    afterAllFinish.storyPointsChangesValues shouldEqual Seq(0, 1, 2, 3)
  }

  // TODO: test na pojawianie się / znikanie tasków technicznych

  private val dateIterator = Stream.from(100, 100).map { i => new Date(i.toLong) }.toIterable.iterator

  private def nextDate = dateIterator.next()

  implicit class EnhancedSprint(sprint: Sprint) {
    def updateTasks(updatedTasks: UserStory*) = sprint.update(updatedTasks, finishSprint = false)(nextDate).updatedSprint

    def storyPointsChangesValues(implicit config: ProjectConfig): Seq[Int] = sprint.columnStatesHistory.map { dateWithStoryPoints =>
      dateWithStoryPoints.storyPointsForColumn(ProjectConfigUtils.closingColumnIndex)
    }
  }

  implicit class EnhancedUserStory(userStory: UserStory) {
    def close(implicit config: ProjectConfig): UserStory = {
      val closedTechnicalTasks = userStory.technicalTasksWithoutParentId.map(_.close)
      userStory.copy(status = ProjectConfigUtils.firstClosingStatus, technicalTasksWithoutParentId = closedTechnicalTasks)
    }

    def reopen(implicit config: ProjectConfig): UserStory = {
      val reopenedTechnicalTasks = userStory.technicalTasksWithoutParentId.map(_.reopen)
      userStory.copy(status = ProjectConfigUtils.firstNotClosingStatus, technicalTasksWithoutParentId = reopenedTechnicalTasks)
    }
  }

  implicit class EnhancedTechnicalTask(technical: TechnicalTask) {
    def close(implicit config: ProjectConfig): TechnicalTask = technical.copy(status = ProjectConfigUtils.firstClosingStatus)

    def reopen(implicit config: ProjectConfig): TechnicalTask = technical.copy(status = ProjectConfigUtils.firstNotClosingStatus)
  }
}


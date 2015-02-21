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
package org.github.microburn.domain

import java.util.Date

import com.typesafe.config.ConfigFactory
import org.github.microburn.{TestConfig, ApplicationContext}
import org.scalatest.{Inside, FlatSpec, Matchers}

class SprintTest extends FlatSpec with Matchers with Inside {

  implicit val config = ProjectConfigUtils.defaultConfig

  it should "produce correct events for update" in {
    val taskInitiallyOpened = SampleTasks.openedUserStory(1)
    val taskInitiallyCompleted = SampleTasks.closedUserStory(2)

    val sprintBeforeUpdate = SampleSprint.withEmptyEvents(taskInitiallyOpened, taskInitiallyCompleted)
    sprintBeforeUpdate.baseStoryPointsForStart shouldEqual BigDecimal(1)

    val sprintAfterFirstFinish = sprintBeforeUpdate.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted)
    sprintAfterFirstFinish.doneStoryPoints shouldEqual Seq(0, 1) // baza 0 bo pomijamy bo tasksDoneOnStartVisibleForHistory

    val sprintAfterSecReopen = sprintAfterFirstFinish.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted.reopen)
    sprintAfterSecReopen.doneStoryPoints shouldEqual Seq(0, 1, 1)
    sprintAfterSecReopen.openedStoryPoints shouldEqual Seq(1, 1, 3)

    val sprintAfterSecFinishOneMoreTime = sprintAfterSecReopen.updateTasks(taskInitiallyOpened.close, taskInitiallyCompleted.close)
    sprintAfterSecFinishOneMoreTime.doneStoryPoints shouldEqual Seq(0, 1, 1, 3)
    sprintAfterSecFinishOneMoreTime.openedStoryPoints shouldEqual Seq(1, 1, 3, 3)
  }

  it should "generate empty events for not estimated technical tasks and non empty for parent user stories" in {
    val technical = SampleTasks.openedTechnicalTask(optionalSP = None)
    val userStory = SampleTasks.openedUserStory(1, Seq(technical))
    val sprint = SampleSprint.withEmptyEvents(userStory)

    val completedUserStory = userStory.copy(technicalTasksWithoutParentId = IndexedSeq(technical.close)).close

    val afterUpdate = sprint.updateTasks(completedUserStory)

    afterUpdate.doneStoryPoints shouldEqual Seq(0, 1)
  }

  it should "generate non empty events for estimated technical tasks and empty for parent user stories" in {
    val firstTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val userStory = SampleTasks.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = SampleSprint.withEmptyEvents(userStory)

    val completedFirstUserStory = userStory.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical.close, secTechnical))
    val afterFirstFinish = sprint.updateTasks(completedFirstUserStory)
    afterFirstFinish.doneStoryPoints shouldEqual Seq(0, 1)

    val completedAllUserStory = completedFirstUserStory.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical.close, secTechnical.close)).close
    val afterAllFinish = afterFirstFinish.updateTasks(completedAllUserStory)
    afterAllFinish.doneStoryPoints shouldEqual Seq(0, 1, 3)
  }

  it should "generate correct events for scope change" in {
    val firstTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val secTechnical = SampleTasks.openedTechnicalTask(optionalSP = Some(1))
    val userStory = SampleTasks.openedUserStory(3, Seq(firstTechnical, secTechnical))
    val sprint = SampleSprint.withEmptyEvents(userStory)

    val secTechnicalClosed = secTechnical.close
    val withSecClosed = userStory.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical, secTechnicalClosed))
    val afterSecClose = sprint.updateTasks(withSecClosed)
    afterSecClose.doneStoryPoints shouldEqual Seq(0, 1)

    val secTechnicalWithChangedScope = secTechnicalClosed.copy(optionalStoryPoints = Some(2))
    val changedScope = withSecClosed.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical, secTechnicalWithChangedScope))
    val afterScopeChange = afterSecClose.updateTasks(changedScope)
    inside(afterScopeChange) {
      case _ => afterScopeChange.doneStoryPoints shouldEqual Seq(0, 1, 2)
    }

    val completedAllUserStory = changedScope.copy(technicalTasksWithoutParentId = IndexedSeq(firstTechnical.close, secTechnicalWithChangedScope)).close
    val afterAllFinish = afterScopeChange.updateTasks(completedAllUserStory)
    afterAllFinish.doneStoryPoints shouldEqual Seq(0, 1, 2, 3)
  }

  private val dateIterator = Stream.from(100, 100).map { i => new Date(i.toLong) }.toIterable.iterator

  private def nextDate = dateIterator.next()

  implicit class EnhancedSprint(sprint: Sprint) {
    def updateTasks(updatedTasks: UserStory*) = sprint.update(updatedTasks, sprint.details.toMajor)(nextDate).updatedSprint

    def doneStoryPoints(implicit config: ProjectConfig): Seq[BigDecimal] = sprint.sprintHistory.columnStates.map { dateWithStoryPoints =>
      dateWithStoryPoints.storyPointsForColumn(config.lastDoneColumn.index)
    }

    def openedStoryPoints(implicit config: ProjectConfig): Seq[BigDecimal] = sprint.sprintHistory.columnStates.map { dateWithStoryPoints =>
      dateWithStoryPoints.storyPointsForColumn(0)
    }
  }

  implicit class EnhancedUserStory(userStory: UserStory) {
    def close(implicit config: ProjectConfig): UserStory = {
      val closedTechnicalTasks = userStory.technicalTasksWithoutParentId.map(_.close)
      userStory.copy(status = TaskCompletedStatus, technicalTasksWithoutParentId = closedTechnicalTasks)
    }

    def reopen(implicit config: ProjectConfig): UserStory = {
      val reopenedTechnicalTasks = userStory.technicalTasksWithoutParentId.map(_.reopen)
      userStory.copy(status = SpecifiedStatus(ProjectConfigUtils.firstNotCompletedStatus), technicalTasksWithoutParentId = reopenedTechnicalTasks)
    }
  }

  implicit class EnhancedTechnicalTask(technical: TechnicalTask) {
    def close(implicit config: ProjectConfig): TechnicalTask = technical.copy(status = TaskCompletedStatus)

    def reopen(implicit config: ProjectConfig): TechnicalTask = technical.copy(status = SpecifiedStatus(ProjectConfigUtils.firstNotCompletedStatus))
  }
}
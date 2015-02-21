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
import ComputationContextConversions._

case class BoardState(userStories: Seq[UserStory], date: Date) extends HavingNestedTasks[UserStory] {
  override type Self = BoardState

  override protected def nestedTasks: Seq[UserStory] = userStories

  def doneTasksIds(implicit config: ProjectConfig): Set[String] =
    (for {
      userStory <- notBacklogUserStories
      task <- userStory.flattenTasks
      configuredTasksBoardColumn <- task.boardColumn
      if configuredTasksBoardColumn.isDoneColumn
    } yield task.taskId).toSet

  def userStoriesStoryPointsSum(implicit context: ComputationContext): BigDecimal =
    (for {
      userStory <- notBacklogUserStories
      if context.isVisible(userStory)
    } yield userStory.storyPointsSum).sum

  def doneTasksStoryPointsSum(implicit context: ComputationContext): BigDecimal =
    tasksForColumnsMatching(_.isDoneColumn).map(_.storyPointsWithoutSubTasks).sum

  def diff(other: BoardState): Seq[TaskEvent] = nestedDiff(other)(other.date)

  def plus(event: TaskEvent): BoardState = event match {
    case a:TaskAdded if !a.isTechnicalTask =>
      new BoardState(userStories :+ UserStory(a), a.date)
    case r:TaskRemoved if !r.isTechnicalTask =>
      require(taskById.contains(r.taskId), s"User story missing: ${r.taskId}")
      new BoardState(userStories.filterNot(_.taskId == r.taskId), r.date)
    case u:TaskUpdated if !u.isTechnicalTask =>
      withUpdatedParentUserStory(u) { parent =>
        parent.copy(
          taskName = u.taskName,
          optionalStoryPoints = u.optionalStoryPoints,
          status = u.status
        )
      }
    case a:TaskAdded =>
      assume(event.isTechnicalTask)
      withUpdatedParentUserStory(a) { parent =>
        parent.add(TechnicalTask(a))
      }
    case r:TaskRemoved =>
      assume(event.isTechnicalTask)
      withUpdatedParentUserStory(r) { parent =>
        parent.remove(r.taskId)
      }
    case u:TaskUpdated =>
      assume(event.isTechnicalTask)
      withUpdatedParentUserStory(u) { parent =>
        parent.update(u.taskId) { technical =>
          technical.copy(
            taskName = u.taskName,
            optionalStoryPoints = u.optionalStoryPoints,
            status = u.status
          )
        }
      }
  }

  private def withUpdatedParentUserStory(event: TaskEvent)
                                        (updateParent: UserStory => UserStory): BoardState = {
    val parent = nestedTask(event.parentUserStoryId).getOrElse {
      throw new IllegalArgumentException(s"User story missing: ${event.parentUserStoryId}")
    }
    val updated = updateParent(parent)
    withUpdateNestedTask(updated).copy(date = event.date)
  }

  override protected def updateNestedTasks(newNestedTasks: Seq[UserStory]): Self = copy(userStories = newNestedTasks)

  def columnsState(implicit context: ComputationContext): DateWithColumnsState = {
    val indexOnSum = context.config.nonBacklogColumns.map(_.index).map { boardColumnIndex =>
      boardColumnIndex -> storyPointsOnRightFromColumn(boardColumnIndex)
    }.toMap
    DateWithColumnsState(date, indexOnSum)
  }

  private def storyPointsOnRightFromColumn(columnIndex: Int)
                                          (implicit context: ComputationContext) =
    tasksForColumnsMatching(_.index >= columnIndex).map(_.storyPointsWithoutSubTasks).sum

  private def tasksForColumnsMatching(matchColumn: BoardColumn => Boolean)
                                     (implicit context: ComputationContext): Seq[Task] = {
    for {
      userStory <- notBacklogUserStories
      task <- userStory.flattenTasks
      if context.isVisible(task)
      configuredTasksBoardColumn <- task.boardColumn
      if matchColumn(configuredTasksBoardColumn)
    } yield task
  }

  private def notBacklogUserStories(implicit config: ProjectConfig): Seq[UserStory] =
    userStories.filter(userStory => userStory.boardColumn.isDefined)

  override def toString: String = {
    userStories.toSeq.sortBy(_.taskId).map(_.toString).mkString(",\n")
  }

}
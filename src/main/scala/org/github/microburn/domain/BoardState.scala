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

case class BoardState(userStories: Seq[UserStory], date: Date) extends HavingNestedTasks[UserStory] {
  override type Self = BoardState

  override protected def nestedTasks: Seq[UserStory] = userStories

  def userStoriesStoryPointsSum(implicit projectConfig: ProjectConfig): BigDecimal = nestedTasksStoryPointsSum

  def userStoriesNotDoneStoryPointsSum(implicit projectConfig: ProjectConfig): BigDecimal = storyPointsForColumnsMatching(!_.isDoneColumn)

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

  def columnsState(implicit config: ProjectConfig): DateWithColumnsState = {
    val indexOnSum = config.boardColumns.map(_.index).map { boardColumnIndex =>
      boardColumnIndex -> storyPointsOnRightFromColumn(boardColumnIndex)
    }.toMap
    DateWithColumnsState(date, indexOnSum)
  }

  private def storyPointsOnRightFromColumn(columnIndex: Int)
                                          (implicit config: ProjectConfig) = {
    storyPointsForColumnsMatching(_.index >= columnIndex)
  }

  private def storyPointsForColumnsMatching(matchColumn: BoardColumn => Boolean)
                                           (implicit config: ProjectConfig) = {
    (for {
      task <- flattenTasks
      if matchColumn(config.boardColumns(task.boardColumnIndex))
    } yield task.storyPointsWithoutSubTasks).sum
  }

  private def flattenTasks: Seq[Task] = for {
    userStory <- userStories
    task <- userStory.flattenTasks
  } yield task

  override def toString: String = {
    userStories.toSeq.sortBy(_.taskId).map(_.toString).mkString(",\n")
  }

}
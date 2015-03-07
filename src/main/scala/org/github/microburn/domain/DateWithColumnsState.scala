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
import scalaz._
import Scalaz._

case class DateWithColumnsState(date: Date, indexOnColumnState: Map[Int, ColumnState]) {
  def plus(const: BigDecimal): DateWithColumnsState = {
    copy(indexOnColumnState = indexOnColumnState.mapValues(_ + const))
  }

  def multiply(const: BigDecimal): DateWithColumnsState = {
    copy(indexOnColumnState = indexOnColumnState.mapValues(_ * const))
  }

  def withoutTaskChanges: DateWithColumnsState = {
    copy(indexOnColumnState = indexOnColumnState.mapValues { state =>
      state.copy(added = Nil, removed = Nil)
    })
  }

  def storyPointsForColumn(boardColumnIndex: Int): BigDecimal = indexOnColumnState.get(boardColumnIndex).map(_.storyPointsSum).getOrElse(0)

  def addedForColumn(boardColumnIndex: Int): Seq[TaskDetails] = indexOnColumnState.get(boardColumnIndex).map(_.added).getOrElse(Nil)

  def removedForColumn(boardColumnIndex: Int): Seq[TaskDetails] = indexOnColumnState.get(boardColumnIndex).map(_.removed).getOrElse(Nil)
}

object DateWithColumnsState {
  def apply(withTasksOnRightFromColumns: DateWithTasksOnRightFromColumns)(implicit config: ProjectConfig): DateWithColumnsState = {
    val columnStates = withTasksOnRightFromColumns.tasksOnsRight.mapValues(ColumnState(_))
    DateWithColumnsState(withTasksOnRightFromColumns.date, columnStates)
  }
}

case class ColumnState(storyPointsSum: BigDecimal, added: Seq[TaskDetails], removed: Seq[TaskDetails]) {
  def +(const: BigDecimal): ColumnState = {
    copy(storyPointsSum = storyPointsSum + const)
  }

  def *(const: BigDecimal): ColumnState = {
    copy(storyPointsSum = storyPointsSum * const)
  }  
}

case class TaskDetails(name: String, parentName: Option[String], storyPoints: BigDecimal)

object TaskDetails {
  def apply(task: Task)(implicit config: ProjectConfig): TaskDetails = {
    TaskDetails(task.taskName, task.optionalParentUserStory.map(_.taskName), task.storyPointsWithoutSubTasks)
  }
}

object ColumnState {
  def apply(all: Seq[Task])(implicit config: ProjectConfig): ColumnState = {
    ColumnState(all, Nil, Nil)
  }

  def apply(all: Seq[Task], added: Seq[Task], removed: Seq[Task])(implicit config: ProjectConfig): ColumnState = {
    val storyPointsSum = all.map(_.storyPointsWithoutSubTasks).sum
    ColumnState(storyPointsSum, added.map(TaskDetails(_)), removed.map(TaskDetails(_)))
  }
}

case class DateWithTasksOnRightFromColumns(date: Date, tasksOnsRight: Map[Int, Seq[Task]]) {
  def diff(other: DateWithTasksOnRightFromColumns)(implicit config: ProjectConfig): DateWithColumnsState = {
    val columnStates = tasksOnsRight.map {
      case (columnIndex, tasks) =>
        val otherTasks = other.tasksOnsRight(columnIndex)
        val added = filterNotExistingInBase(otherTasks, tasks)
        val removed = filterNotExistingInBase(tasks, otherTasks)
        columnIndex -> ColumnState(tasks, added, removed)
    }
    DateWithColumnsState(date, columnStates)
  }

  private def filterNotExistingInBase(base: Seq[Task], tasks: Seq[Task]): Seq[Task] = {
    val baseIds = base.map(_.taskId).toSet
    tasks.filterNot { task => baseIds.contains(task.taskId) }
  }
}


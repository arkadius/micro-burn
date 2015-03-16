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

case class DateWithColumnsChanges(date: Date, indexOnColumnState: Map[Int, ColumnChanges]) {
  def plus(const: BigDecimal): DateWithColumnsChanges = {
    copy(indexOnColumnState = indexOnColumnState.mapValues(_ + const))
  }

  def multiply(const: BigDecimal): DateWithColumnsChanges = {
    copy(indexOnColumnState = indexOnColumnState.mapValues(_ * const))
  }

  def withoutTaskChanges: DateWithColumnsChanges = {
    copy(indexOnColumnState = indexOnColumnState.mapValues { state =>
      state.copy(addedPoints = Nil, removedPoints = Nil)
    })
  }

  def storyPointsForColumn(boardColumnIndex: Int): BigDecimal = indexOnColumnState.get(boardColumnIndex).map(_.storyPointsSum).getOrElse(0)

  def addedForColumn(boardColumnIndex: Int): Seq[TaskChange] = indexOnColumnState.get(boardColumnIndex).map(_.addedPoints).getOrElse(Nil)

  def removedForColumn(boardColumnIndex: Int): Seq[TaskChange] = indexOnColumnState.get(boardColumnIndex).map(_.removedPoints).getOrElse(Nil)
}

object DateWithColumnsChanges {
  def apply(withTasksOnRightFromColumns: DateWithTasksOnRightFromColumns)(implicit config: ProjectConfig): DateWithColumnsChanges = {
    val columnStates = withTasksOnRightFromColumns.tasksOnsRight.mapValues(ColumnChanges(_))
    DateWithColumnsChanges(withTasksOnRightFromColumns.date, columnStates)
  }
}

case class ColumnChanges(storyPointsSum: BigDecimal, addedPoints: Seq[TaskChange], removedPoints: Seq[TaskChange]) {
  def +(const: BigDecimal): ColumnChanges = {
    copy(storyPointsSum = storyPointsSum + const)
  }

  def *(const: BigDecimal): ColumnChanges = {
    copy(storyPointsSum = storyPointsSum * const)
  }  
}

case class TaskChange(id: String, name: String, parentId: Option[String], parentName: Option[String], storyPoints: Double)

object TaskChange {
  def apply(task: Task, storyPointsDiff: BigDecimal): TaskChange = {
    TaskChange(
      id = task.taskId,
      name = task.taskName,
      parentId = task.optionalParentUserStory.map(_.taskId),
      parentName = task.optionalParentUserStory.map(_.taskName),
      storyPoints = storyPointsDiff.toString().toDouble)
  }
}

object ColumnChanges {
  def apply(all: Seq[Task])(implicit config: ProjectConfig): ColumnChanges = {
    ColumnChanges(all, Nil, Nil)
  }

  def apply(all: Seq[Task], addedPoints: Seq[TaskChange], removedPoints: Seq[TaskChange])
           (implicit config: ProjectConfig): ColumnChanges = {
    val storyPointsSum = all.map(_.storyPointsWithoutSubTasks).sum
    ColumnChanges(storyPointsSum, addedPoints, removedPoints)
  }
}

case class DateWithTasksOnRightFromColumns(date: Date, tasksOnsRight: Map[Int, Seq[Task]]) {
  def diff(other: DateWithTasksOnRightFromColumns)
          (implicit config: ProjectConfig): DateWithColumnsChanges = {
    val columnStates = tasksOnsRight.map {
      case (columnIndex, tasks) =>
        val otherTasks = other.tasksOnsRight(columnIndex)
        val added = filterNotExistingOnLeftOrAddedPoints(otherTasks, tasks)
        val removed = filterNotExistingOnLeftOrAddedPoints(tasks, otherTasks)
        columnIndex -> ColumnChanges(tasks, added, removed)
    }
    DateWithColumnsChanges(date, columnStates)
  }

  private def filterNotExistingOnLeftOrAddedPoints(left: Seq[Task], right: Seq[Task])
                                                  (implicit config: ProjectConfig): Seq[TaskChange] = {
    val leftTaskById = left.map { task => task.taskId -> task }.toMap
    def taskAdded(task: Task) = !leftTaskById.contains(task.taskId)
    object AddedPoint {
      def unapply(task: Task): Option[TaskChange] = {
        for {
          leftTask <- leftTaskById.get(task.taskId)
          spDiff = task.storyPointsWithoutSubTasks - leftTask.storyPointsWithoutSubTasks
          if spDiff > BigDecimal(0)
        } yield TaskChange(task, spDiff)
      }
    }
    right.collect {
      case task if taskAdded(task) =>
        TaskChange(task, task.storyPointsWithoutSubTasks)
      case AddedPoint(change) =>
        change
    }
  }

}
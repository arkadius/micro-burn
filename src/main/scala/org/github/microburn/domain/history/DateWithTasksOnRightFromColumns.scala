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
package org.github.microburn.domain.history

import java.util.Date

import org.github.microburn.domain._

case class DateWithTasksOnRightFromColumns(date: Date, tasksOnsRight: Map[Int, Seq[Task]]) {
  def diff(other: DateWithTasksOnRightFromColumns)
          (implicit config: ProjectConfig): DateWithColumnsChanges = {
    val columnStates = tasksOnsRight.map {
      case (columnIndex, tasks) =>
        val otherTasks = other.tasksOnsRight(columnIndex)
        val added = prepareForNotExistingOnLeftOrAddedPoints(otherTasks, tasks)
        val removed = prepareForNotExistingOnLeftOrAddedPoints(tasks, otherTasks)
        columnIndex -> ColumnChanges(tasks, added, removed)
    }
    DateWithColumnsChanges(date, columnStates)
  }

  private def prepareForNotExistingOnLeftOrAddedPoints(left: Seq[Task], right: Seq[Task])
                                                      (implicit config: ProjectConfig): Seq[UserStoryChange] = {
    val filtered = filterNotExistingOnLeftOrAddedPoints(left, right)
    val (_, grouped) = filtered.groupBy(_.id).unzip
    grouped.map { group =>
      group.reduce(_ merge _)
    }.toSeq
  }

  private def filterNotExistingOnLeftOrAddedPoints(left: Seq[Task], right: Seq[Task])
                                                  (implicit config: ProjectConfig): Seq[UserStoryChange] = {
    val leftTaskById = left.map { task => task.taskId -> task }.toMap
    def taskAdded(task: Task) = !leftTaskById.contains(task.taskId)
    object AddedPoint {
      def unapply(task: Task): Option[UserStoryChange] = {
        for {
          leftTask <- leftTaskById.get(task.taskId)
          spDiff = task.storyPointsWithoutSubTasks - leftTask.storyPointsWithoutSubTasks
          if spDiff > BigDecimal(0)
        } yield UserStoryChange(task, spDiff)
      }
    }
    right.collect {
      case task if taskAdded(task) =>
        UserStoryChange(task, task.storyPointsWithoutSubTasks)
      case AddedPoint(change) =>
        change
    }
  }
}
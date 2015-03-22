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

  def addedForColumn(boardColumnIndex: Int): Seq[UserStoryChange] = indexOnColumnState.get(boardColumnIndex).map(_.addedPoints).getOrElse(Nil)

  def removedForColumn(boardColumnIndex: Int): Seq[UserStoryChange] = indexOnColumnState.get(boardColumnIndex).map(_.removedPoints).getOrElse(Nil)
}

case class ColumnChanges(storyPointsSum: BigDecimal, addedPoints: Seq[UserStoryChange], removedPoints: Seq[UserStoryChange]) {
  def +(const: BigDecimal): ColumnChanges = {
    copy(storyPointsSum = storyPointsSum + const)
  }

  def *(const: BigDecimal): ColumnChanges = {
    copy(storyPointsSum = storyPointsSum * const)
  }
}

case class UserStoryChange(id: String, name: String, technical: List[TechnicalTaskChange], storyPoints: Option[Double]) {
  def storyPointsSum: Double = storyPoints.getOrElse(0d) + technical.map(_.storyPoints).sum

  def merge(other: UserStoryChange): UserStoryChange = {
    require(other.id == id)
    require(other.name == name)
    require(storyPoints.isEmpty || other.storyPoints.isEmpty)
    copy(technical = other.technical ::: technical, storyPoints = storyPoints.orElse(other.storyPoints))
  }

  def withSortedTechnical: UserStoryChange = copy(technical = technical.sortBy(tech => (-tech.storyPoints, tech.name)))
}

case class TechnicalTaskChange(name: String, storyPoints: Double)

object DateWithColumnsChanges {
  def apply(withTasksOnRightFromColumns: DateWithTasksOnRightFromColumns)(implicit config: ProjectConfig): DateWithColumnsChanges = {
    val columnStates = withTasksOnRightFromColumns.tasksOnsRight.mapValues(ColumnChanges(_))
    DateWithColumnsChanges(withTasksOnRightFromColumns.date, columnStates)
  }
}

object ColumnChanges {
  def apply(all: Seq[Task])(implicit config: ProjectConfig): ColumnChanges = {
    ColumnChanges(all, Nil, Nil)
  }

  def apply(all: Seq[Task], addedPoints: Seq[UserStoryChange], removedPoints: Seq[UserStoryChange])
           (implicit config: ProjectConfig): ColumnChanges = {
    val storyPointsSum = all.map(_.storyPointsWithoutSubTasks).sum
    ColumnChanges(storyPointsSum, addedPoints, removedPoints)
  }
}

object UserStoryChange {
  def apply(task: Task, storyPointsDiff: BigDecimal): UserStoryChange = {
    task.optionalParentUserStory.map { parentUserStory =>
      val technicalChange = TechnicalTaskChange(task, storyPointsDiff)
      UserStoryChange(parentUserStory.taskId, parentUserStory.taskName, List(technicalChange), None)
    }.getOrElse {
      UserStoryChange(task.taskId, task.taskName, Nil, Some(storyPointsDiff.toString().toDouble))
    }
  }
}

object TechnicalTaskChange {
  def apply(task: Task, storyPointsDiff: BigDecimal): TechnicalTaskChange = {
    TechnicalTaskChange(task.taskName, storyPointsDiff.toString().toDouble)
  }
}
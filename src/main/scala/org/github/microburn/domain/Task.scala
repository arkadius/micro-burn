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

import scala.math.BigDecimal.RoundingMode
import scalaz._
import Scalaz._

sealed trait Task { self =>
  def taskId: String
  def parentUserStoryId: String
  def isTechnicalTask: Boolean

  def taskName: String
  def optionalStoryPoints: Option[BigDecimal]
  def status: TaskStatus

  def taskAdded(implicit timestamp: Date): Seq[TaskAdded]
  def storyPointsWithoutSubTasks(implicit config: ProjectConfig): BigDecimal

  def storyPointsOfSelf(implicit config: ProjectConfig): BigDecimal

  def boardColumn(implicit config: ProjectConfig): Option[BoardColumn] = status match {
    case SpecifiedStatus(status) => config.boardColumn(status)
    case TaskCompletedStatus => Some(config.lastDoneColumn)
  }
}

case class UserStory(taskId: String,
                     taskName: String,
                     optionalStoryPoints: Option[BigDecimal],
                     technicalTasksWithoutParentId: IndexedSeq[TechnicalTask],
                     status: TaskStatus) extends Task with ComparableWith[UserStory] with HavingNestedTasks[TechnicalTaskWithParent] {
  private final val SP_SPLITTED_BETWEEN_TECHICAL_SCALE: Int = 1

  override type Self = UserStory

  protected val nestedTasks: Seq[TechnicalTaskWithParent] = technicalTasksWithoutParentId.map(TechnicalTaskWithParent(_, this))

  override def parentUserStoryId: String = taskId

  override def isTechnicalTask: Boolean = false

  override def taskAdded(implicit timestamp: Date): Seq[TaskAdded] = flattenTasks.map(TaskAdded(_))

  def add(technical: TechnicalTask): UserStory = {
    require(!taskById.contains(technical.taskId))
    copy(technicalTasksWithoutParentId = technicalTasksWithoutParentId :+ technical)
  }

  def remove(taskId: String): UserStory = {
    require(taskById.contains(taskId))
    copy(technicalTasksWithoutParentId = technicalTasksWithoutParentId.filterNot(_.taskId == taskId))
  }

  def update(taskId: String)(updateTechnical: TechnicalTask => TechnicalTask): UserStory = {
    val updated = updateTechnical(taskById(taskId).technical)
    withUpdateNestedTask(TechnicalTaskWithParent(updated, this))
  }

  override protected def updateNestedTasks(newNestedTasks: Seq[TechnicalTaskWithParent]): Self =
    copy(technicalTasksWithoutParentId = newNestedTasks.map(_.technical).toIndexedSeq)

  override def diff(other: Self)(implicit timestamp: Date): Seq[TaskEvent] = {
    selfDiff(other) ++ nestedDiff(other)
  }

  def flattenTasks: List[Task] = this :: nestedTasks.toList

  override def storyPointsOfSelf(implicit config: ProjectConfig): BigDecimal  = {
    optionalStoryPoints orElse
      config.defaultStoryPointsForUserStrories getOrElse
      BigDecimal(0)
  }

  override def storyPointsWithoutSubTasks(implicit config: ProjectConfig): BigDecimal = {
    val diff = storyPointsOfSelf - nestedTasksStoryPointsSum
    diff.max(BigDecimal(0))
  }

  def storyPointsToSplitPerTechnical(implicit config: ProjectConfig): BigDecimal = {
    if (config.splitSpBetweenTechnicalTasks) {
      val definedTechnicalPoints = technicalTasksWithoutParentId.flatMap(_.optionalStoryPoints).sum
      val sumPointsToSplitBetweenTechnical = (storyPointsOfSelf - definedTechnicalPoints).max(0)
      val technicalWithoutDefinedSp = technicalTasksWithoutParentId.count(_.optionalStoryPoints.isEmpty)
      if (technicalWithoutDefinedSp > 0)
        (sumPointsToSplitBetweenTechnical / technicalWithoutDefinedSp).setScale(SP_SPLITTED_BETWEEN_TECHICAL_SCALE, RoundingMode.FLOOR)
      else
        BigDecimal(0)
    } else {
      BigDecimal(0)
    }
  }

  override def toString: String = {
    s"""UserStory(id = ${taskId.take(5)}
       |${technicalTasksWithoutParentId.map(_.toString).mkString(",\n")}
       |)""".stripMargin
  }

}

case class TechnicalTaskWithParent(technical: TechnicalTask,
                                   parent: UserStory) extends Task with ComparableWith[TechnicalTaskWithParent] {
  override def parentUserStoryId = parent.taskId

  override def taskId: String = technical.taskId
  override def taskName: String = technical.taskName
  override def optionalStoryPoints: Option[BigDecimal] = technical.optionalStoryPoints
  override def status: TaskStatus = technical.status

  override def isTechnicalTask: Boolean = true

  override def storyPointsOfSelf(implicit config: ProjectConfig): BigDecimal  = {
    optionalStoryPoints getOrElse parent.storyPointsToSplitPerTechnical
  }

  override def storyPointsWithoutSubTasks(implicit config: ProjectConfig): BigDecimal = storyPointsOfSelf

  override def taskAdded(implicit timestamp: Date): Seq[TaskAdded] = Seq(TaskAdded(this))

  override def diff(other: TechnicalTaskWithParent)(implicit timestamp: Date): Seq[TaskEvent] = {
    selfDiff(other)
  }
}

trait ComparableWith[OtherTaskType <: Task with ComparableWith[_]] { self: Task =>
  def diff(other: OtherTaskType)(implicit timestamp: Date): Seq[TaskEvent]

  protected def selfDiff(other: OtherTaskType)(implicit timestamp: Date): Seq[TaskEvent] = {
    (other.taskName != this.taskName ||
      other.optionalStoryPoints != this.optionalStoryPoints ||
      other.status != this.status).option(TaskUpdated(other)).toSeq
  }
}

case class TechnicalTask(taskId: String,
                         taskName: String,
                         optionalStoryPoints: Option[BigDecimal],
                         status: TaskStatus) {
  override def toString: String = {
    s"  Technical(${taskId.take(5)})"
  }
}

object UserStory {
  def apply(added: TaskAdded): UserStory = {
    require(!added.isTechnicalTask, s"Invalid event $added")
    require(added.parentUserStoryId == added.taskId, s"Invalid event $added")
    UserStory(
      taskId = added.taskId,
      taskName = added.taskName,
      optionalStoryPoints = added.optionalStoryPoints,
      technicalTasksWithoutParentId = IndexedSeq.empty,
      status = added.status)
  }
}

object TechnicalTask {
  def apply(added: TaskAdded): TechnicalTask = {
    require(added.isTechnicalTask, s"Invalid event $added")
    TechnicalTask(
      taskId = added.taskId,
      taskName = added.taskName,
      optionalStoryPoints = added.optionalStoryPoints,
      status = added.status)
  }
}
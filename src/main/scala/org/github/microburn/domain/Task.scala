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

import org.github.microburn.domain.history.KnowledgeAboutRecentlyDoneTasks

import scala.math.BigDecimal.RoundingMode
import scalaz.Scalaz._

sealed trait Task { self =>
  def taskId: String
  def optionalParentUserStory: Option[UserStory]
  def isTechnicalTask: Boolean

  def taskName: String
  def optionalStoryPoints: Option[BigDecimal]
  def status: TaskStatus

  def taskAdded(implicit timestamp: Date): Seq[TaskAdded]

  def storyPointsWithoutSubTasks(implicit config: ProjectConfig): BigDecimal

  def isInSprint(implicit config: ProjectConfig, knowledge: KnowledgeAboutRecentlyDoneTasks) = boardColumn.exists(!_.isBacklogColumn)

  def boardColumn(implicit config: ProjectConfig, knowledge: KnowledgeAboutRecentlyDoneTasks): Option[BoardColumn] =
    status match {
      case TaskCompletedStatus =>
        Some(config.lastDoneColumn)
      case TaskOpenedStatus =>
        Some(config.firstNotDoneSprintColumn)
      case SpecifiedStatus(name) =>
        config.boardColumn(name).orElse {
          knowledge.recentlyWasDone(this).option(config.lastDoneColumn) // done column for recently done task in not configured column
        }
    }
}

case class UserStory(taskId: String,
                     taskName: String,
                     optionalStoryPoints: Option[BigDecimal],
                     technicalTasksWithoutParentId: IndexedSeq[TechnicalTask],
                     status: TaskStatus)
  extends Task with ComparableWith[UserStory] with Openable[UserStory] with HavingNestedTasks[TechnicalTaskWithParent] {

  private final val SP_SPLITTED_BETWEEN_TECHICAL_SCALE: Int = 1

  override type Self = UserStory

  protected val nestedTasks: Seq[TechnicalTaskWithParent] = technicalTasksWithoutParentId.map(TechnicalTaskWithParent(_, this))

  override def optionalParentUserStory: Option[UserStory] = None

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

  override def flattenTasks: Seq[Task] = this +: nestedTasks

  override def storyPointsWithoutSubTasks(implicit config: ProjectConfig): BigDecimal = {
    val diff = storyPointsOfSelf - nestedTasks.map(_.storyPointsWithoutSubTasks).sum
    diff.max(BigDecimal(0))
  }

  def storyPointsOfSelf(implicit config: ProjectConfig): BigDecimal  = {
    optionalStoryPoints orElse
      config.defaultStoryPointsForUserStories getOrElse
      BigDecimal(0)
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

  override def open(implicit config: ProjectConfig, knowledge: KnowledgeAboutRecentlyDoneTasks): Self = openSelf.openNestedInSprint

  override protected def updateStatus(status: TaskStatus): UserStory = copy(status = status)

  override def toString: String = {
    s"""UserStory(id = ${taskId.take(5)}
       |${technicalTasksWithoutParentId.map(_.toString).mkString(",\n")}
       |)""".stripMargin
  }

}

case class TechnicalTaskWithParent(technical: TechnicalTask,
                                   parent: UserStory)
  extends Task with ComparableWith[TechnicalTaskWithParent] with Openable[TechnicalTaskWithParent] {

  override def optionalParentUserStory: Option[UserStory] = Some(parent)

  override def taskId: String = technical.taskId
  override def taskName: String = technical.taskName
  override def optionalStoryPoints: Option[BigDecimal] = technical.optionalStoryPoints
  override def status: TaskStatus = technical.status

  override def isTechnicalTask: Boolean = true

  override def storyPointsWithoutSubTasks(implicit config: ProjectConfig): BigDecimal =
    optionalStoryPoints getOrElse parent.storyPointsToSplitPerTechnical

  override def taskAdded(implicit timestamp: Date): Seq[TaskAdded] = Seq(TaskAdded(this))

  override def diff(other: TechnicalTaskWithParent)(implicit timestamp: Date): Seq[TaskEvent] = {
    selfDiff(other)
  }

  override def open(implicit config: ProjectConfig, knowledge: KnowledgeAboutRecentlyDoneTasks): TechnicalTaskWithParent = openSelf

  override protected def updateStatus(status: TaskStatus): TechnicalTaskWithParent = copy(technical = technical.copy(status = status))
}

trait ComparableWith[OtherTaskType <: Task with ComparableWith[OtherTaskType]] { self: Task =>
  def diff(other: OtherTaskType)(implicit timestamp: Date): Seq[TaskEvent]

  protected def selfDiff(other: OtherTaskType)(implicit timestamp: Date): Seq[TaskEvent] = {
    (other.taskName != this.taskName ||
      other.optionalStoryPoints != this.optionalStoryPoints ||
      other.status != this.status).option(TaskUpdated(other)).toSeq
  }
}

trait Openable[Self <: Task with Openable[Self]] { self: Task =>
  def open(implicit config: ProjectConfig, knowledge: KnowledgeAboutRecentlyDoneTasks): Self

  protected def openSelf: Self = updateStatus(TaskOpenedStatus)

  protected def updateStatus(status: TaskStatus): Self
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
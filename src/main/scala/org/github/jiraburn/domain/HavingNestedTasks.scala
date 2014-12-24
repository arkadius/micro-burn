package org.github.jiraburn.domain

import java.util.Date

trait HavingNestedTasks[NestedTaskType <: Task with ComparableWith[NestedTaskType]] { self =>
  type Self >: self.type <: HavingNestedTasks[NestedTaskType]

  protected def nestedTasks: Set[NestedTaskType]
  
  protected lazy val taskById: Map[String, NestedTaskType] =
    nestedTasks.groupBy(_.taskId).mapValues(_.head).toMap

  private def tasksIds: Set[String] = taskById.keySet

  protected def nestedTask(taskId: String): Option[NestedTaskType] = taskById.get(taskId)

  protected def nestedTasksStoryPointsSum: Int = {
    nestedTasks.flatMap { task =>
      task.optionalStoryPoints
    }.sum
  }

  def nestedDiff(other: Self)(implicit timestamp: Date): Seq[TaskEvent] = {
    val allTaskIds = this.tasksIds ++ other.tasksIds
    for {
      taskId <- allTaskIds.toSeq
      optionalThisTask = this.nestedTask(taskId)
      optionalOtherTask = other.nestedTask(taskId)
      event <- diff(optionalThisTask, optionalOtherTask)
    } yield event
  }

  private def diff(thisTask: Option[NestedTaskType], otherTask: Option[NestedTaskType])
                  (implicit timestamp: Date): Seq[TaskEvent] = {
    (thisTask, otherTask) match {
      case (None, None) => throw new IllegalArgumentException("At least one story should be defined")
      case (None, Some(definedOtherTask)) => definedOtherTask.taskAdded
      case (Some(definedThisTask), None)  => Seq(TaskRemoved(definedThisTask))
      case (Some(definedThisTask), Some(definedOtherTask)) => definedThisTask.diff(definedOtherTask)
    }
  }
}
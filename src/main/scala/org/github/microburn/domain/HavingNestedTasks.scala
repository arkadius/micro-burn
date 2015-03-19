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

trait HavingNestedTasks[NestedTaskType <: Task with ComparableWith[NestedTaskType] with Openable[NestedTaskType]] { self =>
  type Self >: self.type <: HavingNestedTasks[NestedTaskType]

  protected def nestedTasks: Seq[NestedTaskType]
  
  protected lazy val taskById: Map[String, NestedTaskType] =
    nestedTasks.toSeq.map { task => task.taskId -> task }.toMap

  private def tasksIds: Set[String] = taskById.keySet

  protected def nestedTask(taskId: String): Option[NestedTaskType] = taskById.get(taskId)

  def withUpdateNestedTask(updated: NestedTaskType): Self = {
    val currentIndex = nestedTasks.zipWithIndex.collectFirst {
      case (userStory, index) if userStory.taskId == updated.taskId => index
    }
    updateNestedTasks(nestedTasks.updated(currentIndex.get, updated))
  }

  protected def updateNestedTasks(newNestedTasks: Seq[NestedTaskType]): Self

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

  def openNestedInSprint(implicit config: ProjectConfig, knowledge: KnowledgeAboutLastState): Self = {
    val openedNested = nestedTasks.map { nested =>
      if (nested.isInSprint)
        nested.open
      else
        nested
    }
    updateNestedTasks(openedNested)
  }
}
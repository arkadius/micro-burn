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

import scala.language.implicitConversions

case class ComputationContext(private val tasksVisibilityDeterminer: TaskVisibilityDeterminer, config: ProjectConfig) {
  def isVisible(task: Task) = tasksVisibilityDeterminer.isVisible(task)(config)
}

object ComputationContext {
  def apply(initialAfterStartPlusAcceptableDelay: Boolean, initiallyDoneTaskIds: Set[String])(implicit config: ProjectConfig): ComputationContext = {
    val tasksVisibilityDeterminer = TaskVisibilityDeterminer(initialAfterStartPlusAcceptableDelay, initiallyDoneTaskIds)
    ComputationContext(tasksVisibilityDeterminer, config)
  }
}

object ComputationContextConversions {
  implicit def contextToConfig(implicit context: ComputationContext): ProjectConfig = context.config
}

sealed trait TaskVisibilityDeterminer {
  def isVisible(task: Task)
               (implicit config: ProjectConfig): Boolean
}

case object AllTasksVisible extends TaskVisibilityDeterminer {
  override def isVisible(task: Task)
                        (implicit config: ProjectConfig): Boolean = true
}

case class TasksDoneOnStartVisibleOnlyIfReopened(tasksDoneOnStartIds: Set[String]) extends TaskVisibilityDeterminer {
  override def isVisible(task: Task)(implicit config: ProjectConfig): Boolean = {
    !tasksDoneOnStartIds.contains(task.taskId) || isReopened(task)
  }

  private def isReopened(task: Task)(implicit config: ProjectConfig): Boolean = !task.boardColumn.exists(_.isDoneColumn)
}

object TaskVisibilityDeterminer {
  def apply(initialAfterStartPlusAcceptableDelay: Boolean, initiallyDoneTaskIds: Set[String]): TaskVisibilityDeterminer = {
    if (initialAfterStartPlusAcceptableDelay)
      AllTasksVisible
    else
      TasksDoneOnStartVisibleOnlyIfReopened(initiallyDoneTaskIds)
  }
}
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

sealed trait TaskEvent {
  def taskId: String
  def parentUserStoryId: String
  def isTechnicalTask: Boolean
  def date: Date
}

case class TaskAdded(taskId: String,
                     parentUserStoryId: String,
                     isTechnicalTask: Boolean,
                     taskName: String,
                     optionalStoryPoints: Option[BigDecimal],
                     status: String,
                     date: Date) extends TaskEvent

case class TaskRemoved(taskId: String,
                       parentUserStoryId: String,
                       isTechnicalTask: Boolean,
                       date: Date) extends TaskEvent

case class TaskUpdated(taskId: String,
                       parentUserStoryId: String,
                       isTechnicalTask: Boolean,
                       taskName: String,
                       optionalStoryPoints: Option[BigDecimal],
                       status: String,
                       date: Date) extends TaskEvent

object TaskAdded {
  def apply(task: Task)(implicit timestamp: Date): TaskAdded = TaskAdded(
    taskId = task.taskId,
    parentUserStoryId = task.parentUserStoryId,
    isTechnicalTask = task.isTechnicalTask,
    taskName = task.taskName,
    optionalStoryPoints = task.optionalStoryPoints,
    status = task.status,
    date = timestamp)
}

object TaskRemoved {
  def apply(task: Task)(implicit timestamp: Date): TaskRemoved = TaskRemoved(
    taskId = task.taskId,
    parentUserStoryId = task.parentUserStoryId,
    isTechnicalTask = task.isTechnicalTask,
    date = timestamp)
}

object TaskUpdated {
  def apply(task: Task)(implicit timestamp: Date): TaskUpdated = TaskUpdated(
    taskId = task.taskId,
    parentUserStoryId = task.parentUserStoryId,
    isTechnicalTask = task.isTechnicalTask,
    taskName = task.taskName,
    optionalStoryPoints = task.optionalStoryPoints,
    status = task.status,
    date = timestamp)
}
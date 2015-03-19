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

class SprintHistoricalKnowledge private (tasksDoneAndNotReopenedInPrevStates: Set[String])
                                        (implicit val aboutLastState: KnowledgeAboutLastState) {

  def shouldBeUsedInCalculations(task: Task)(implicit config: ProjectConfig): Boolean =
    task.isInSprint && (!tasksDoneAndNotReopenedInPrevStates.contains(task.taskId) || isNotDone(task))

  private def isNotDone(task: Task)(implicit config: ProjectConfig): Boolean =
    !task.boardColumn.exists(_.isDoneColumn)

  def withNextStateDoneTaskIds(nextStateDoneTaskIds: Set[String]): SprintHistoricalKnowledge = {
    val newTasksDoneAndNotReopenedInPrevStates = tasksDoneAndNotReopenedInPrevStates intersect nextStateDoneTaskIds
    val newKnowledgeAboutLastState = new KnowledgeAboutLastState(nextStateDoneTaskIds)
    new SprintHistoricalKnowledge(newTasksDoneAndNotReopenedInPrevStates)(newKnowledgeAboutLastState)
  }
}

class KnowledgeAboutLastState(tasksRecentlyDone: Set[String]) {
  def recentlyWasDone(task: Task): Boolean = tasksRecentlyDone.contains(task.taskId)
}

object SprintHistoricalKnowledge {
  def assumingAllDoneTasksWereNotReopened(doneTaskIds: Set[String]): SprintHistoricalKnowledge =
    new SprintHistoricalKnowledge(doneTaskIds)(new KnowledgeAboutLastState(doneTaskIds))
}

object KnowledgeAboutLastState {
  def assumingNoneDoneTask: KnowledgeAboutLastState =
    new KnowledgeAboutLastState(Set.empty)
}
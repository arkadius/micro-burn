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

import org.github.microburn.domain.{UserStory, BoardState, ProjectConfig, Task}

class SprintHistoricalKnowledge private (tasksDoneAndNotReopenedInPrevStates: Seq[Task])
                                        (implicit val aboutLastState: KnowledgeAboutLastState) {
  private val tasksDoneAndNotReopenedInPrevStatesIds: Set[String] = tasksDoneAndNotReopenedInPrevStates.map(_.taskId).toSet

  def shouldBeUsedInCalculations(task: Task)(implicit config: ProjectConfig): Boolean = {
    def isNotDone = !task.boardColumn.exists(_.isDoneColumn)
    def isDoneAndNotReopenedInPrevStates = !tasksDoneAndNotReopenedInPrevStatesIds.contains(task.taskId)
    task.isInSprint && (isNotDone || isDoneAndNotReopenedInPrevStates)
  }

  def doneTasksOutOfBoardStoryPointsSum(board: BoardState)
                                       (implicit config: ProjectConfig): BigDecimal = {
    implicit val implicitKnowledge = this
    aboutLastState.doneTasksOutOfBoard(board).map(_.storyPointsWithoutSubTasks).sum
  }

  def doneTasksOutOfBoard(board: BoardState)
                         (implicit config: ProjectConfig): Seq[Task] = {
    implicit val implicitKnowledge = this
    aboutLastState.doneTasksOutOfBoard(board)
  }

  def withNextStateDoneTasks(nextStateDoneTasks: Seq[Task]): SprintHistoricalKnowledge = {
    val newTasksDoneAndNotReopenedInPrevStates = nextStateDoneTasks.filter(task => tasksDoneAndNotReopenedInPrevStatesIds.contains(task.taskId))
    val newTasksDoneWithoutNotReopenedInPrevStates = nextStateDoneTasks.filterNot(task => tasksDoneAndNotReopenedInPrevStatesIds.contains(task.taskId))
    val newKnowledgeAboutLastState = new KnowledgeAboutLastState(newTasksDoneWithoutNotReopenedInPrevStates)
    new SprintHistoricalKnowledge(newTasksDoneAndNotReopenedInPrevStates)(newKnowledgeAboutLastState)
  }
}

class KnowledgeAboutLastState(tasksRecentlyDone: Seq[Task]) {
  private val tasksRecentlyDoneIds: Set[String] =  tasksRecentlyDone.map(_.taskId).toSet
  
  def recentlyWasDone(task: Task): Boolean = tasksRecentlyDoneIds.contains(task.taskId)

  def doneTasksOutOfBoard(board: BoardState)
                         (implicit config: ProjectConfig, knowledge: SprintHistoricalKnowledge): Seq[Task] = {
    val boardTaskIds = board.tasks.map(_.taskId).toSet
    tasksRecentlyDone.filterNot(task => boardTaskIds.contains(task.taskId))
  }
}

object SprintHistoricalKnowledge {
  def assumingAllDoneTasksWereNotReopened(doneTasks: Seq[Task]): SprintHistoricalKnowledge =
    new SprintHistoricalKnowledge(doneTasks)(new KnowledgeAboutLastState(Seq.empty))
}

object KnowledgeAboutLastState {
  def assumingNoneDoneTask: KnowledgeAboutLastState =
    new KnowledgeAboutLastState(Seq.empty)
}
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

import org.github.microburn.domain.history.{KnowledgeAboutLastState, SprintHistoricalKnowledge}

case class BoardState(userStories: Seq[UserStory], date: Date) extends HavingNestedTasks[UserStory] {
  override type Self = BoardState

  override protected def nestedTasks: Seq[UserStory] = userStories

  def diff(other: BoardState): Seq[TaskEvent] = nestedDiff(other)(other.date)

  def plus(event: TaskEvent): BoardState = event match {
    case a:TaskAdded if !a.isTechnicalTask =>
      new BoardState(userStories :+ UserStory(a), a.date)
    case r:TaskRemoved if !r.isTechnicalTask =>
      require(taskById.contains(r.taskId), s"User story missing: ${r.taskId}")
      new BoardState(userStories.filterNot(_.taskId == r.taskId), r.date)
    case u:TaskUpdated if !u.isTechnicalTask =>
      withUpdatedParentUserStory(u) { parent =>
        parent.copy(
          taskName = u.taskName,
          optionalStoryPoints = u.optionalStoryPoints,
          status = u.status
        )
      }
    case a:TaskAdded =>
      assume(event.isTechnicalTask)
      withUpdatedParentUserStory(a) { parent =>
        parent.add(TechnicalTask(a))
      }
    case r:TaskRemoved =>
      assume(event.isTechnicalTask)
      withUpdatedParentUserStory(r) { parent =>
        parent.remove(r.taskId)
      }
    case u:TaskUpdated =>
      assume(event.isTechnicalTask)
      withUpdatedParentUserStory(u) { parent =>
        parent.update(u.taskId) { technical =>
          technical.copy(
            taskName = u.taskName,
            optionalStoryPoints = u.optionalStoryPoints,
            status = u.status
          )
        }
      }
  }

  private def withUpdatedParentUserStory(event: TaskEvent)
                                        (updateParent: UserStory => UserStory): BoardState = {
    val parentUserStoryId = event.optionalParentUserStoryId.getOrElse(event.taskId)
    val parent = nestedTask(parentUserStoryId).getOrElse {
      throw new IllegalArgumentException(s"User story missing: $parentUserStoryId")
    }
    val updated = updateParent(parent)
    withUpdateNestedTask(updated).copy(date = event.date)
  }

  override protected def updateNestedTasks(newNestedTasks: Seq[UserStory]): Self = copy(userStories = newNestedTasks)

  def doneTasks(knowledge: KnowledgeAboutLastState)(implicit config: ProjectConfig): Seq[Task] = {
    implicit val knowledgeImplicit = knowledge
    for {
      userStory <- userStories
      if userStory.isInSprint
      task <- userStory.flattenTasks
      if task.isInSprint
      configuredTasksBoardColumn <- task.boardColumn
      if configuredTasksBoardColumn.isDoneColumn
    } yield task
  }

  def storyPointsSum(implicit config: ProjectConfig, knowledge: SprintHistoricalKnowledge): BigDecimal =
    tasks.map(_.storyPointsWithoutSubTasks).sum

  def tasks(implicit config: ProjectConfig, knowledge: SprintHistoricalKnowledge): Seq[Task] =
    tasksWithMatchingColumn(_ => true)

  def tasksOnRightFromColumn(columnIndex: Int)
                            (implicit config: ProjectConfig, knowledge: SprintHistoricalKnowledge): Seq[Task] =
    tasksWithMatchingColumn(_.index >= columnIndex)

  private def tasksWithMatchingColumn(columnMatches: BoardColumn => Boolean)
                                     (implicit config: ProjectConfig, knowledge: SprintHistoricalKnowledge): Seq[Task] = {
    import knowledge.aboutLastState
    for {
      userStory <- userStories
      if knowledge.shouldBeUsedInCalculations(userStory)
      task <- userStory.flattenTasks
      if knowledge.shouldBeUsedInCalculations(task)
      configuredTasksBoardColumn <- task.boardColumn
      if columnMatches(configuredTasksBoardColumn)
    } yield task
  }

  override def toString: String = {
    userStories.toSeq.sortBy(_.taskId).map(_.toString).mkString(",\n")
  }
}
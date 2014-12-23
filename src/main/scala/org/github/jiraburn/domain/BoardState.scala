package org.github.jiraburn.domain

import java.util.Date

case class BoardState(nestedTasks: Seq[UserStory], date: Date) extends HavingNestedTasks[UserStory] {
  override type Self = BoardState

  def userStoriesStoryPointsSum: Int = nestedTasksStoryPointsSum

  def diff(other: BoardState): Seq[TaskEvent] = super.diff(other)(other.date)

  def plus(event: TaskEvent): BoardState = event match {
    case a:TaskAdded if !a.isTechnicalTask =>
      new BoardState(nestedTasks :+ UserStory(a), a.date)
    case r:TaskRemoved if !r.isTechnicalTask =>
      require(nestedTask(r.taskId).isDefined, s"User story missing: ${r.taskId}")
      new BoardState(nestedTasks.filterNot(_.taskId == r.taskId), r.date)
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
    val parent = nestedTask(event.parentUserStoryId).getOrElse {
      throw new IllegalArgumentException(s"User story missing: ${event.parentUserStoryId}")
    }
    val updated = updateParent(parent)
    new BoardState(userStoriesWithUpdateTask(updated), event.date)
  }

  private def userStoriesWithUpdateTask(updated: UserStory) = {
    nestedTasks.filterNot(_.taskId == updated.taskId) :+ updated
  }

  def columnsState(implicit config: ProjectConfig): DateWithColumnsState = {
    val indexOnSum = config.boardColumns.map(_.index).map { boardColumnIndex =>
      boardColumnIndex -> storyPointsOnRightFromColumn(boardColumnIndex)
    }.toMap
    DateWithColumnsState(date, indexOnSum)
  }

  private def storyPointsOnRightFromColumn(columnIndex: Int)
                                          (implicit config: ProjectConfig) = {
    (for {
      userStory <- nestedTasks
      task <- userStory.flattenTasks
      if task.boardColumnIndex >= columnIndex
    } yield task.storyPointsWithoutSubTasks).sum
  }

}
package org.github.jiraburn.domain

import java.util.Date

import scalaz.Scalaz._

case class BoardState(userStories: Seq[UserStory], date: Date) {

  private[this] val taskStates: Map[String, TaskWithState] = {
    val flattenTasks = for {
      userStory <- userStories
      task <- userStory.technicalTasks :+ userStory
    } yield task
    flattenTasks.groupBy(_.taskId).mapValues { tasks =>
      TaskWithState(tasks.head)
    }
  }

  def userStoriesStoryPointsSum: Int = {
    userStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def diff(other: BoardState): Seq[TaskChanged] = {
    val allTaskIds = this.taskIds ++ other.taskIds
    for {
      taskId <- allTaskIds.toSeq
      optionalCurrentTask = this.taskState(taskId)
      optionalUpdatedTask = other.taskState(taskId)
      event <- diff(optionalCurrentTask, optionalUpdatedTask)(other.date)
    } yield event
  }

  private def taskIds: Set[String] = taskStates.keySet

  private def taskState(taskId: String): Option[TaskWithState] = taskStates.get(taskId)

  private def diff(state: Option[TaskWithState], nextState: Option[TaskWithState])
                  (implicit timestamp: Date): Option[TaskChanged] = {
    Seq(state, nextState).flatten match {
      case Nil => None
      case one :: Nil => Some(prepareEvent(state, nextState))
      case definedState :: definedNextState :: Nil =>
        val statusChanged = definedNextState.status != definedState.status
        val storyPointsChanged = definedNextState.storyPoints != definedState.storyPoints
        // co ze zmianą parenta/typu zadania?
        (statusChanged || storyPointsChanged).option(prepareEvent(state, nextState))
    }
  }

  private def prepareEvent(state: Option[TaskWithState], nextState: Option[TaskWithState])
                          (implicit timestamp: Date): TaskChanged = {
    val task = (nextState orElse state).get
    TaskChanged(
      task.taskId, task.parentTaskId, task.isTechnicalTask,
      nextState.map(_.state), timestamp
    )
  }
  
  def plus(change: TaskChanged): BoardState = { // TODO: eventy powinny pokazywać zmiany domenowe, a nie pochodnego bytu
//    val newStates = change.optionalToState match {
//      case None => taskStates - change.taskId
//      case Some(toState) => taskStates + (change.taskId -> TaskWithState(change.taskId, change.parentTaskId, change.isTechnicalTask, toState))
//    }
//    new BoardState(newStates, change.date)
    null
  }


  def columnsState(implicit config: ProjectConfig): DateWithColumnsState = {
    val indexOnSum = config.boardColumns.map(_.index).map { boardColumnIndex =>
      boardColumnIndex -> storyPointsOnRightFromBoardColumn(boardColumnIndex)
    }.toMap
    DateWithColumnsState(date, indexOnSum)
  }

  private def storyPointsOnRightFromBoardColumn(columnIndex: Int)
                                               (implicit config: ProjectConfig) =
    taskStates.values.filter(_.boardColumnIndex >= columnIndex).map(_.storyPoints).sum

}

case class TaskWithState(taskId: String,
                         parentTaskId: String,
                         isTechnicalTask: Boolean,
                         state: TaskState) {
  
  def status: Int = state.status
  def storyPoints: Int = state.storyPoints  
  
  def boardColumnIndex(implicit config: ProjectConfig): Int = config.boardColumnIndex(status)
}

object TaskWithState {
  def apply(task: Task): TaskWithState = {
    TaskWithState(task.taskId, task.parentUserStoryId, task.isTechnicalTask, TaskState(task.status, task.storyPointsWithoutSubTasks))
  }
}

case class TaskState(status: Int, storyPoints: Int)
package org.github.jiraburn.domain

import java.util.Date

sealed trait Task {
  def taskId: String
  def taskName: String
  def storyPointsWithoutSubTasks: Int
  def parentUserStoryId: String
  protected def status: Int

  def isOpened(implicit config: ProjectConfig) = config.isOpening(status)
  def isCompleted(implicit config: ProjectConfig) = config.isClosing(status)

  def finish(parentUserStoryFromInitialScope: Boolean)
            (implicit config: ProjectConfig, timestamp: Date): Option[TaskCompleted] =
    if (isOpened)
      Some(doFinish(parentUserStoryFromInitialScope))
    else
      None
  
  def reopen(parentUserStoryFromInitialScope: Boolean)
            (implicit config: ProjectConfig, timestamp: Date): Option[TaskReopened] =
    if (isCompleted)
      Some(doReopen(parentUserStoryFromInitialScope))
    else
      None

  private def doFinish(parentUserStoryFromInitialScope: Boolean)
                      (implicit timestamp: Date): TaskCompleted =
    TaskCompleted(taskId, parentUserStoryId, parentUserStoryFromInitialScope, timestamp, storyPointsWithoutSubTasks)
  
  private def doReopen(parentUserStoryFromInitialScope: Boolean)
                      (implicit timestamp: Date): TaskReopened =
    TaskReopened(taskId, parentUserStoryId, parentUserStoryFromInitialScope, timestamp, storyPointsWithoutSubTasks)
}

case class UserStory(taskId: String,
                     taskName: String,
                     optionalStoryPoints: Option[Int],
                     technicalTasksWithoutParentId: List[TechnicalTask],
                     status: Int) extends Task {

  def technicalTasks: Seq[Task] = technicalTasksWithoutParentId.map(TechnicalTaskWithParentId(_, taskId))
  
  override def storyPointsWithoutSubTasks: Int = {
    val storyPointsOfMine = optionalStoryPoints.getOrElse(0)
    val subTasksStoryPoints = technicalTasks.map(_.storyPointsWithoutSubTasks).sum
    val diff = storyPointsOfMine - subTasksStoryPoints
    Math.max(0, diff)
  }

  override def parentUserStoryId: String = taskId
}

case class TechnicalTaskWithParentId(technical: TechnicalTask, parentUserStoryId: String) extends Task {
  override def taskId: String = technical.taskId

  override def taskName: String = technical.taskName

  override def storyPointsWithoutSubTasks: Int = technical.optionalStoryPoints.getOrElse(0)

  override protected def status: Int = technical.status
}

case class TechnicalTask(taskId: String, taskName: String, optionalStoryPoints: Option[Int], status: Int)

object UserStory {
  def apply(taskId: String,
            taskName: String,
            optionalStoryPoints: Option[Int],
            technicalTasks: Seq[TechnicalTask])
           (implicit config: ProjectConfig): UserStory = {
    new UserStory(taskId, taskName, optionalStoryPoints, technicalTasks.toList, config.firstClosingStatus)
  }
}

object TechnicalTask {
  def apply(taskId: String,
            taskName: String,
            optionalStoryPoints: Option[Int])
           (implicit config: ProjectConfig): TechnicalTask = {
    new TechnicalTask(taskId, taskName, optionalStoryPoints, config.firstOpeningStatus)
  }
}
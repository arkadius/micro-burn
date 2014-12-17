package org.github.jiraburn.domain

import java.util.Date
import scalaz._
import Scalaz._

sealed trait Task {
  def taskId: String
  def taskName: String
  def storyPointsWithoutSubTasks: Int
  def parentUserStoryId: String
  protected def status: Int

  def delta(nextState: Task)
           (implicit timestamp: Date): Option[TaskChanged] = {
    require(nextState.taskId == taskId)
    val statusChanged = nextState.status != status
    val storyPointsWithoutSubTasksChanged = nextState.storyPointsWithoutSubTasks != storyPointsWithoutSubTasks
    (statusChanged || storyPointsWithoutSubTasksChanged).option(prepareDelta(nextState))
  }

  protected def prepareDelta(nextState: Task)
                            (implicit timestamp: Date): TaskChanged = {
    TaskChanged(
      taskId, parentUserStoryId,
      status, nextState.status,
      storyPointsWithoutSubTasks, nextState.storyPointsWithoutSubTasks,
      timestamp
    )
  }
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
    new TechnicalTask(taskId, taskName, optionalStoryPoints, config.firstNotClosingStatus)
  }
}
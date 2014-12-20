package org.github.jiraburn.domain

sealed trait Task {
  def taskId: String
  def taskName: String
  def isTechnicalTask: Boolean
  def storyPointsWithoutSubTasks: Int
  def parentUserStoryId: String
  def status: Int
}

case class UserStory(taskId: String,
                     taskName: String,
                     optionalStoryPoints: Option[Int],
                     technicalTasksWithoutParentId: List[TechnicalTask],
                     status: Int) extends Task {

  override def isTechnicalTask: Boolean = false

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

  override def isTechnicalTask: Boolean = true

  override def storyPointsWithoutSubTasks: Int = technical.optionalStoryPoints.getOrElse(0)

  override def status: Int = technical.status
}

case class TechnicalTask(taskId: String, taskName: String, optionalStoryPoints: Option[Int], status: Int)
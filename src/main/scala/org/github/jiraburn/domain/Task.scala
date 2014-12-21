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


  def close(implicit config: ProjectConfig): UserStory = {
    val closedTechnicalTasks = technicalTasksWithoutParentId.map(_.close)
    copy(status = config.firstClosingStatus, technicalTasksWithoutParentId = closedTechnicalTasks)
  }

  def reopen(implicit config: ProjectConfig): UserStory = {
    val reopenedTechnicalTasks = technicalTasksWithoutParentId.map(_.reopen)
    copy(status = config.firstNotClosingStatus, technicalTasksWithoutParentId = reopenedTechnicalTasks)
  }
}

case class TechnicalTaskWithParentId(technical: TechnicalTask, parentUserStoryId: String) extends Task {
  override def taskId: String = technical.taskId

  override def taskName: String = technical.taskName

  override def isTechnicalTask: Boolean = true

  override def storyPointsWithoutSubTasks: Int = technical.optionalStoryPoints.getOrElse(0)

  override def status: Int = technical.status
}

case class TechnicalTask(taskId: String, taskName: String, optionalStoryPoints: Option[Int], status: Int) {
  def close(implicit config: ProjectConfig): TechnicalTask = copy(status = config.firstClosingStatus)

  def reopen(implicit config: ProjectConfig): TechnicalTask = copy(status = config.firstNotClosingStatus)
}
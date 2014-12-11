package com.example.domain

import java.util.Date

sealed trait Task {
  def taskId: String
  def storyPointsWithoutSubtasks: Int
  protected def state: TaskState

  def isOpened = state == Opened
  def isCompleted = state == Completed

  def finish(implicit timestamp: Date): Option[TaskCompleted] =
    if (isOpened)
      Some(doFinish)
    else
      None
  
  def reopen(implicit timestamp: Date): Option[TaskReopened] =
    if (isCompleted)
      Some(doReopen)
    else
      None

  private def doFinish(implicit timestamp: Date): TaskCompleted = TaskCompleted(taskId, timestamp, storyPointsWithoutSubtasks)
  private def doReopen(implicit timestamp: Date): TaskReopened  = TaskReopened(taskId, timestamp, storyPointsWithoutSubtasks)
}

case class UserStory(taskId: String, optionalStoryPoints: Option[Int], state: TaskState, technicalTasks: Seq[TechnicalTask]) extends Task {
  override def storyPointsWithoutSubtasks: Int = {
    val storyPointsOfMine = optionalStoryPoints.getOrElse(0)
    val subtasksStoryPoints = technicalTasks.map(_.storyPointsWithoutSubtasks).sum
    val diff = storyPointsOfMine - subtasksStoryPoints
    Math.max(0, diff)
  }
}

case class TechnicalTask(taskId: String, optionalStoryPoints: Option[Int], state: TaskState) extends Task {
  override def storyPointsWithoutSubtasks: Int = optionalStoryPoints.getOrElse(0)
}
package com.example.domain

import java.util.Date

sealed trait Task {
  def taskId: String
  def optionalStoryPoints: Option[Int]
  protected def state: TaskState

  def isOpened = state == Opened
  def isCompleted = state == Completed
  
  def finish(implicit timestamp: Date): TaskCompleted
  def reopen(implicit timestamp: Date): TaskReopened

  protected def finishCountingPointsOfMine(implicit timestamp: Date): TaskCompleted = TaskCompleted(taskId, timestamp, optionalStoryPoints.getOrElse(0))
  protected def reopenCountingPointsOfMine(implicit timestamp: Date): TaskReopened = TaskReopened(taskId, timestamp, optionalStoryPoints.getOrElse(0))
}

case class UserStory(taskId: String, optionalStoryPoints: Option[Int], state: TaskState, technicalTasks: Seq[TechnicalTask]) extends Task {
  override def finish(implicit timestamp: Date): TaskCompleted = {
    if (containsEstimatedTechnicalTasks)
      TaskCompleted(taskId, timestamp, 0)
    else
      finishCountingPointsOfMine(timestamp)
  }

  override def reopen(implicit timestamp: Date): TaskReopened = {
    if (containsEstimatedTechnicalTasks)
      TaskReopened(taskId, timestamp, 0)
    else
      reopenCountingPointsOfMine(timestamp)
  }

  private def containsEstimatedTechnicalTasks: Boolean = technicalTasks.exists(_.optionalStoryPoints.isDefined)
}

case class TechnicalTask(taskId: String, optionalStoryPoints: Option[Int], state: TaskState) extends Task {
  override def finish(implicit timestamp: Date): TaskCompleted = finishCountingPointsOfMine(timestamp)
  override def reopen(implicit timestamp: Date): TaskReopened = reopenCountingPointsOfMine(timestamp)
}
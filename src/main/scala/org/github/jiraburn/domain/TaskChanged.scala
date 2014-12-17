package org.github.jiraburn.domain

import java.util.Date

import scalaz.Scalaz._

case class TaskChanged(taskId: String,
                       parentTaskId: String,
                       fromStatus: Int,
                       toStatus: Int,
                       fromStoryPoints: Int,
                       toStoryPoints: Int,
                       date: Date) {
  
  def taskWasReopen(implicit config: ProjectConfig) =
    config.isClosing(fromStatus) && config.isNotClosing(toStatus)

  def taskWasClosed(implicit config: ProjectConfig) =
    config.isNotClosing(fromStatus) && config.isClosing(toStatus)
  
  def storyPointsWereChanged = {
    val diff = toStoryPoints - fromStatus
    (diff != 0).option(diff)
  }

  def storyPointsAfterChange = toStoryPoints
  
  def isOpenedAfterChange(implicit config: ProjectConfig) = config.isNotClosing(toStatus)
  
  def isClosedAfterChange(implicit config: ProjectConfig) = config.isClosing(toStatus) 
  
}

case class DateWithStoryPoints(date: Date, storyPoints: Int) {
  def accumulateWithEvent(event: TaskChanged)(implicit config: ProjectConfig) = {
    val maxDate = if (event.date.after(date)) {
      event.date
    } else {
      date
    }
    val storyPointsDiff =
      if (event.taskWasReopen) {
        +event.storyPointsAfterChange
      } else if (event.taskWasClosed) {
        -event.storyPointsAfterChange
      } else {
        event.storyPointsWereChanged.map { diff =>
          if (event.isClosedAfterChange)
            -diff
          else
            +diff
        }.getOrElse(0)
      } 
        
        
    DateWithStoryPoints(maxDate, storyPoints + storyPointsDiff)
  }
}

object DateWithStoryPoints {
  def zero: DateWithStoryPoints = DateWithStoryPoints(new Date(0L), 0)
}
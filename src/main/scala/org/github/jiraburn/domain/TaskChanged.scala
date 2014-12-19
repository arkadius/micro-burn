package org.github.jiraburn.domain

import java.util.Date
import scalaz._
import Scalaz._

case class TaskChanged(taskId: String,
                       parentTaskId: String,
                       isTechnicalTask: Boolean,
                       optionalFromState: Option[TaskState],
                       optionalToState: Option[TaskState],
                       date: Date) {

  def balanceChangeForContinuouslyExistingTask(implicit config: ProjectConfig): Option[Int] = {
    for {
      fromState <- optionalFromState
      toState <- optionalToState       
    } yield toState.storyPointsForStatusIs(closing = toState.statusIsClosing) - fromState.storyPointsForStatusIs(closing = toState.statusIsClosing)
  }

  def balanceChangeBecauseOfExistenceChange(implicit config: ProjectConfig): Option[Int] = {
    (optionalFromState, optionalToState) match {
      case (None, Some(toState))   => Some(toState.storyPointChangeDependingOnStatus)
      case (Some(fromState), None) => Some(fromState.storyPointChangeDependingOnStatus)
      case _ => None
    }
  }
}

case class TaskState(status: Int, storyPoints: Int) {
  def storyPointsForStatusIs(closing: Boolean)(implicit config: ProjectConfig): Int =
    (closing && statusIsClosing || !closing && !statusIsClosing).option {
      storyPointChangeDependingOnGivenStatusIsClosing(closing)
    }.getOrElse(0)

  def storyPointChangeDependingOnStatus(implicit config: ProjectConfig) = storyPointChangeDependingOnGivenStatusIsClosing(statusIsClosing)

  private def storyPointChangeDependingOnGivenStatusIsClosing(givenStatusIsClosing: Boolean): Int =
    if (givenStatusIsClosing)
      -storyPoints
    else
      +storyPoints

  def statusIsClosing(implicit config: ProjectConfig): Boolean = config.isClosing(status)
}

object TaskState {
  def apply(task: Task): TaskState = TaskState(task.status, task.storyPointsWithoutSubTasks)
}

case class DateWithStoryPoints(date: Date, storyPoints: Int) {
  def accumulateWithEvent(event: TaskChanged)(implicit config: ProjectConfig) = {
    val maxDate = if (event.date.after(date)) {
      event.date
    } else {
      date
    }
    val storyPointsDiff = Seq(
      event.balanceChangeForContinuouslyExistingTask,
      event.balanceChangeBecauseOfExistenceChange.filter(_ => event.isTechnicalTask)
    ).flatten.sum
    // nie sumujemy punktów za zmiany istnienia historyjek na tablicy, bo mogą to być zmiany
    // poszerzające/zmniejszające zakres sprintu nie wpływające na spalanie, albo modyfikacja statusu
    // mogła być wykonana poza tablicą
        
    DateWithStoryPoints(maxDate, storyPoints + storyPointsDiff)
  }
}

object DateWithStoryPoints {
  def zero: DateWithStoryPoints = DateWithStoryPoints(new Date(0L), 0)
}
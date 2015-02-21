package org.github.microburn.domain

sealed trait TaskVisibilityDeterminer {
  def isVisible(task: Task): Boolean
}

case object AllTasksVisible extends TaskVisibilityDeterminer {
  override def isVisible(task: Task): Boolean = true
}

class TasksDoneOnStartNotVisible(tasksDoneOnStartIds: Set[String]) extends TaskVisibilityDeterminer {
  override def isVisible(task: Task): Boolean = !tasksDoneOnStartIds.contains(task.taskId)
}

object TaskVisibilityDeterminer {
  def apply(initialAfterStartPlusAcceptableDelay: Boolean, initiallyDoneTaskIds: Set[String])(implicit config: ProjectConfig): TaskVisibilityDeterminer = {
    if (config.tasksDoneOnStartVisibleForHistory || initialAfterStartPlusAcceptableDelay)
      AllTasksVisible
    else
      new TasksDoneOnStartNotVisible(initiallyDoneTaskIds)
  }
}
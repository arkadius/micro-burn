package org.github.microburn.domain

object ProjectConfigUtils {
  def closingColumnIndex(implicit config: ProjectConfig): Int = config.boardColumnIndex(firstClosingStatus)

  def firstNotClosingStatus(implicit config: ProjectConfig): Int = config.boardColumns.head.statusIds.head

  def firstClosingStatus(implicit config: ProjectConfig): Int = config.boardColumns.last.statusIds.head
}
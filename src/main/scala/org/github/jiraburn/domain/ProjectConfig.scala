package org.github.jiraburn.domain

import com.typesafe.config.Config

import scalaz._
import Scalaz._

case class ProjectConfig(boardColumns: List[BoardColumn]) {
  private val statuses = (for {
    column <- boardColumns
    status <- column.statusIds
  } yield status -> column).toMap

  def closingColumnIndex: Int = boardColumnIndex(firstClosingStatus)

  def boardColumnIndex(status: Int): Int = statuses(status).index

  def firstNotClosingStatus: Int = statuses.toSeq.collect {
    case (id, info) if !info.isClosing => id
  }.sorted.head

  def firstClosingStatus: Int = statuses.toSeq.collect {
    case (id, info) if info.isClosing => id
  }.sorted.head
}

case class BoardColumn(index: Int, name: String, statusIds: List[Int], isClosing: Boolean)

object ProjectConfig {
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = for {
      (columnConfig, index) <- config.getConfigList("jira.greenhopper.boardColumns").zipWithIndex
      name = columnConfig.getString("name")
      statusIds = columnConfig.getIntList("statusIds").map(_.toInt).toList
      closing = columnConfig.hasPath("closing").option(columnConfig.getBoolean("closing")).getOrElse(false)
    } yield BoardColumn(index, name, statusIds, closing)
    ProjectConfig(columns.toList)
  }
}
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

  def firstNotClosingStatus: Int = boardColumns.head.statusIds.head

  def firstClosingStatus: Int = boardColumns.last.statusIds.head
}

case class BoardColumn(index: Int, name: String, statusIds: List[Int], color: String)

object ProjectConfig {
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = for {
      (columnConfig, index) <- config.getConfigList("jira.greenhopper.boardColumns").zipWithIndex
      name = columnConfig.getString("name")
      statusIds = columnConfig.getIntList("statusIds").map(_.toInt).toList
      color = columnConfig.hasPath("color").option(columnConfig.getString("color")).getOrElse("")
    } yield BoardColumn(index, name, statusIds, color)
    ProjectConfig(columns.toList)
  }
}
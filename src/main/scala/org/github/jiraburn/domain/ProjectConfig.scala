package org.github.jiraburn.domain

import com.typesafe.config.Config

import scalaz._
import Scalaz._

case class ProjectConfig(boardColumns: List[BoardColumn]) {
  private val statuses = (for {
    column <- boardColumns
    status <- column.statusIds
  } yield status -> StatusInfo(column.isOpening, column.isClosing, column.name)).toMap

  def isOpening(status: Int) = statuses(status).isOpening

  def isClosing(status: Int) = statuses(status).isClosing

  def nameFromBoard(status: Int) = statuses(status).nameFromBoard

  def firstOpeningStatus: Int = statuses.toSeq.collect {
    case (id, StatusInfo(true, _, _)) => id
  }.sorted.head

  def firstClosingStatus: Int = statuses.toSeq.collect {
    case (id, StatusInfo(_, true, _)) => id
  }.sorted.head
}

case class BoardColumn(name: String, statusIds: List[Int], isOpening: Boolean, isClosing: Boolean)

case class StatusInfo(isOpening: Boolean, isClosing: Boolean, nameFromBoard: String)

object ProjectConfig {
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = for {
      columnConfig <- config.getConfigList("jira.greenhopper.boardColumns")
      name = columnConfig.getString("name")
      statusIds = columnConfig.getIntList("statusIds").map(_.toInt).toList
      opening = columnConfig.hasPath("opening").option(columnConfig.getBoolean("opening")).getOrElse(false)
      closing = columnConfig.hasPath("closing").option(columnConfig.getBoolean("closing")).getOrElse(false)
    } yield BoardColumn(name, statusIds, opening, closing)
    ProjectConfig(columns.toList)
  }
}
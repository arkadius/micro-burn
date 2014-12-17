package org.github.jiraburn.domain

import com.typesafe.config.Config

import scalaz._
import Scalaz._

case class ProjectConfig(boardColumns: List[BoardColumn]) {
  private val statuses = (for {
    column <- boardColumns
    status <- column.statusIds
  } yield status -> StatusInfo(column.isClosing, column.name)).toMap

  def isNotClosing(status: Int) = !isClosing(status)

  def isClosing(status: Int) = statuses.get(status).exists(_.isClosing)

  def nameFromBoard(status: Int): Option[String] = statuses.get(status).map(_.nameFromBoard)

  def firstNotClosingStatus: Int = statuses.toSeq.collect {
    case (id, info) if !info.isClosing => id
  }.sorted.head

  def firstClosingStatus: Int = statuses.toSeq.collect {
    case (id, info) if info.isClosing => id
  }.sorted.head
}

case class BoardColumn(name: String, statusIds: List[Int], isClosing: Boolean)

case class StatusInfo(isClosing: Boolean, nameFromBoard: String)

object ProjectConfig {
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = for {
      columnConfig <- config.getConfigList("jira.greenhopper.boardColumns")
      name = columnConfig.getString("name")
      statusIds = columnConfig.getIntList("statusIds").map(_.toInt).toList
      closing = columnConfig.hasPath("closing").option(columnConfig.getBoolean("closing")).getOrElse(false)
    } yield BoardColumn(name, statusIds, closing)
    ProjectConfig(columns.toList)
  }
}
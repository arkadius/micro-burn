package org.github.microburn.domain

import java.io.File

import com.typesafe.config.Config

import scalaz._
import Scalaz._

case class ProjectConfig(boardColumns: List[BoardColumn], dataRoot: File) {
  private val statuses = (for {
    column <- boardColumns
    status <- column.statusIds
  } yield status -> column).toMap

  def boardColumnIndex(status: Int): Int = statuses(status).index
}

case class BoardColumn(index: Int, name: String, statusIds: List[Int])

object ProjectConfig {
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = for {
      (columnConfig, index) <- config.getConfigList("board.columns").zipWithIndex
      name = columnConfig.getString("name")
      statusIds = columnConfig.getIntList("statusIds").map(_.toInt).toList
    } yield BoardColumn(index, name, statusIds)
    val dataRoot = new File(config.getString("data.project.root"))
    ProjectConfig(columns.toList, dataRoot)
  }
}
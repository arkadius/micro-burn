/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.domain

import java.io.File

import com.typesafe.config.Config

import scalaz._
import Scalaz._

case class ProjectConfig(boardColumns: List[BoardColumn],
                         dataRoot: File,
                         defaultStoryPointsForUserStrories: Option[BigDecimal],
                         splitSpBetweenTechnicalTasks: Boolean) {

  private val statuses = (for {
    column <- boardColumns
    status <- column.statusIds
  } yield status -> column).toMap

  def containsStatus(status: String): Boolean = statuses.contains(status)

  def boardColumnIndex(status: String): Int = statuses(status).index

  def lastDoneColumnIndex: Int = {
    val doneColumns = boardColumns.filter(_.isDoneColumn)
    if (doneColumns.nonEmpty)
      doneColumns.last.index
    else
      boardColumns.last.index
  }
}

case class BoardColumn(index: Int, name: String, statusIds: List[String], isBacklogColumn: Boolean, isDoneColumn: Boolean)

object ProjectConfig {
  import org.github.microburn.util.config.ConfigExtensions._
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = parseColumns(config)
    val defaultStoryPointsForUserStrories = config.optional("defaultStoryPointsForUserStrories")(_.getBigDecimal)
    val splitSpBetweenTechnicalTasks = config.getDefinedBoolean("splitSpBetweenTechnicalTasks")
    val dataRoot = new File(config.getString("dataRoot"))
    ProjectConfig(
      boardColumns = columns,
      dataRoot = dataRoot,
      defaultStoryPointsForUserStrories = defaultStoryPointsForUserStrories,
      splitSpBetweenTechnicalTasks = splitSpBetweenTechnicalTasks)
  }
  
  private def parseColumns(config: Config): List[BoardColumn] = {
    (for {
      (columnConfig, index) <- config.getConfigList("boardColumns").zipWithIndex
      name = columnConfig.getString("name")
      statusIds = statusesFromStatusIds(columnConfig) orElse statusesFromId(columnConfig) getOrElse {
        throw new scala.IllegalArgumentException("Missing field: statusIds or id")
      }
      isBacklogColumn = columnConfig.getDefinedBoolean("backlogColumn")
      isDoneColumn = columnConfig.getDefinedBoolean("doneColumn")
    } yield BoardColumn(
      index = index,
      name = name,
      statusIds = statusIds,
      isBacklogColumn = isBacklogColumn,
      isDoneColumn = isDoneColumn
    )).toList
  }

  private def statusesFromStatusIds(columnConfig: Config): Option[List[String]] = {
    columnConfig.optional("statusIds")(_.getStringList).map(_.toList)
  }

  private def statusesFromId(columnConfig: Config): Option[List[String]] = {
    columnConfig.optional("id")(_.getString).map(List(_))
  }
}
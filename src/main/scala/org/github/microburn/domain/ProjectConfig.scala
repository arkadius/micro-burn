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

case class ProjectConfig(boardColumns: List[BoardColumn], dataRoot: File) {
  private val statuses = (for {
    column <- boardColumns
    status <- column.statusIds
  } yield status -> column).toMap

  def boardColumnIndex(status: String): Int = statuses(status).index
}

case class BoardColumn(index: Int, name: String, statusIds: List[String])

object ProjectConfig {
  import collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val columns = for {
      (columnConfig, index) <- config.getConfigList("board.columns").zipWithIndex
      name = columnConfig.getString("name")
      statusIds = columnConfig.getStringList("statusIds").toList
    } yield BoardColumn(index, name, statusIds)
    val dataRoot = new File(config.getString("data.project.root"))
    ProjectConfig(columns.toList, dataRoot)
  }
}
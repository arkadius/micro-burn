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
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration._

case class ProjectConfig(nonBacklogColumns: List[BoardColumn],
                         dataRoot: File,
                         defaultStoryPointsForUserStories: Option[BigDecimal],
                         splitSpBetweenTechnicalTasks: Boolean,
                         initialFetchAfterSprintStartAcceptableDelay: FiniteDuration,
                         initiallyDoneNotVisibleForBoardState: Boolean,
                         dayOfWeekWeights: IndexedSeq[BigDecimal],
                         scrumManagementMode: ScrumManagementMode,
                         sprintBaseDetermineMode: SprintBaseDetermineMode) {

  private val statuses = (for {
    column <- nonBacklogColumns
    status <- column.statusIds
  } yield status -> column).toMap

  def boardColumn(status: String): Option[BoardColumn] = statuses.get(status)

  def lastDoneColumn: BoardColumn = {
    val doneColumns = nonBacklogColumns.filter(_.isDoneColumn)
    doneColumns.last
  }
}

case class BoardColumn(index: Int, name: String, statusIds: List[String], isDoneColumn: Boolean)

object ProjectConfig {
  import org.github.microburn.util.config.ConfigEnrichments._

  import scala.collection.convert.wrapAll._

  def apply(config: Config): ProjectConfig = {
    val nonBacklogColumns = parseNonBacklogColumns(config)
    require(nonBacklogColumns.size >= 2, "You must define at least two column")
    val nonBacklogColumnsWithAtLeastOneDone = makeAtLeastOneColumnDone(nonBacklogColumns)
    val defaultStoryPointsForUserStrories = config.optional(_.getBigDecimal, "defaultStoryPointsForUserStrories")
    val splitSpBetweenTechnicalTasks = config.getBoolean("splitSpBetweenTechnicalTasks")
    val initialFetchAfterSprintStartAcceptableDelay = config.getDuration("initialFetchAfterSprintStartAcceptableDelay", TimeUnit.MILLISECONDS).millis
    val initiallyDoneNotVisibleForBoardState = config.getBoolean("initiallyDoneNotVisibleForBoardState")
    val dataRoot = new File(config.getString("dataRoot"))
    val scrumManagementMode = ScrumManagementModeParser.parse(config)
    val sprintBaseDetermineMode = SprintBaseDetermineModeParser.parse(config) getOrElse (scrumManagementMode match {
      case ManualManagementMode => AutomaticOnSprintStartMode
      case auto: AutomaticManagementMode => AutomaticOnScopeChangeMode
    })

    ProjectConfig(
      nonBacklogColumns = nonBacklogColumnsWithAtLeastOneDone,
      dataRoot = dataRoot,
      defaultStoryPointsForUserStories = defaultStoryPointsForUserStrories,
      splitSpBetweenTechnicalTasks = splitSpBetweenTechnicalTasks,
      initialFetchAfterSprintStartAcceptableDelay = initialFetchAfterSprintStartAcceptableDelay,
      initiallyDoneNotVisibleForBoardState = initiallyDoneNotVisibleForBoardState,
      dayOfWeekWeights = parseDayOfWeekWeights(config),
      scrumManagementMode = scrumManagementMode,
      sprintBaseDetermineMode = sprintBaseDetermineMode
    )
  }

  private def makeAtLeastOneColumnDone(columns: List[BoardColumn]): List[BoardColumn] = {
    if (!columns.exists(_.isDoneColumn))
      columns.updated(columns.size - 1, columns(columns.size - 1).copy(isDoneColumn = true))
    else
      columns
  }

  private def parseNonBacklogColumns(config: Config): List[BoardColumn] = {
    (for {
      (columnConfig, index) <- config.getConfigList("boardColumns").zipWithIndex
      name = columnConfig.getString("name")
      statusIds = statusesFromStatusIds(columnConfig) orElse statusesFromId(columnConfig) getOrElse {
        throw new scala.IllegalArgumentException("Missing field: statusIds or id")
      }
      isBacklogColumn = columnConfig.getDefinedBoolean("backlogColumn")
      if !isBacklogColumn
      isDoneColumn = columnConfig.getDefinedBoolean("doneColumn")
    } yield BoardColumn(
      index = index,
      name = name,
      statusIds = statusIds,
      isDoneColumn = isDoneColumn
    )).toList
  }

  private def statusesFromStatusIds(columnConfig: Config): Option[List[String]] = {
    columnConfig.optional(_.getStringList, "statusIds").map(_.toList)
  }

  private def statusesFromId(columnConfig: Config): Option[List[String]] = {
    columnConfig.optional(_.getString, "id").map(List(_))
  }
  
  private def parseDayOfWeekWeights(config: Config): IndexedSeq[BigDecimal] = {
    val weights = config.getBigDecimalList("dayOfWeekWeights")
    require(weights.size == 7, "All days of weeks must be defined")
    require(!weights.exists(_ < 0), "Weights must be positive value")
    require(weights.exists(_ > 0), "At least one non zero weight must be defined")
    normalize(weights.toSeq).toIndexedSeq
  }

  private def normalize(seq: Seq[BigDecimal]): Seq[BigDecimal] = {
    val sum = seq.sum
    seq.map(_ / sum)
  }
}
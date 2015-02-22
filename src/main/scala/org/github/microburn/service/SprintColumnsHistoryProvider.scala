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
package org.github.microburn.service

import java.util.Date

import net.liftmodules.ng.Angular.NgModel
import net.liftweb.actor.LAFuture
import net.liftweb.common.Box
import org.github.microburn.domain.actors.{GetStoryPointsHistory, ProjectActor}
import org.github.microburn.domain.{DateWithColumnsState, ProjectConfig, SprintHistory}
import org.joda.time.DateTime

import scalaz.Scalaz._

class SprintColumnsHistoryProvider(projectActor: ProjectActor)
                                  (implicit config: ProjectConfig) {
  import org.github.microburn.util.concurrent.ActorEnrichments._
  import org.github.microburn.util.concurrent.FutureEnrichments._
  
  def columnsHistory(sprintId: Int): LAFuture[Box[ColumnsHistory]] = {
    (projectActor ?? GetStoryPointsHistory(sprintId)).mapTo[Box[SprintHistory]].map { historyBox =>
      historyBox.map(extractColumnsHistory)
    }
  }

  private def extractColumnsHistory(history: SprintHistory): ColumnsHistory = {
    val toAppend = computeToAppend(history).toSeq
    val fullHistory = history.columnStates ++ toAppend

    val baseIndexOnSum = DateWithColumnsState.constIndexOnSum(history.sprintBase)
    val withBaseAdded = fullHistory.map(_.multiply(-1).plus(baseIndexOnSum))

    val columnsHistory = unzipByColumn(withBaseAdded)
    val startDate = new DateTime(history.sprintDetails.start)
    val endDate = new DateTime(history.sprintDetails.end)

    val estimate = computeEstimate(startDate, endDate, history.sprintBase)

    val columns = estimate :: columnsHistory
    ColumnsHistory(startDate.getMillis, columns)
  }

  private def computeToAppend(history: SprintHistory): Option[DateWithColumnsState] = {
    val nowOrSprintsEndForFinished = history.sprintDetails.isFinished.option(history.sprintDetails.end).getOrElse(new Date)
    val last = history.columnStates.last
    val lastAvailableBeforeNow = last.date.before(nowOrSprintsEndForFinished)
    lastAvailableBeforeNow.option {
      last.copy(date = nowOrSprintsEndForFinished)
    }
  }

  private def unzipByColumn(zipped: Seq[DateWithColumnsState]): List[ColumnHistory] = {
    config.nonBacklogColumns.map { column =>
      val storyPointsForColumn = zipped.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        HistoryProbe(allColumnsInfo.date, storyPoints)
      }.toList
      ColumnHistory(column.name, column.isDoneColumn, storyPointsForColumn)
    }
  }

  private def computeEstimate(start: DateTime, end: DateTime, storyPointsSum: BigDecimal): ColumnHistory = {
    val estimates = EstimateComputer.estimatesBetween(start, end, storyPointsSum).map { probe =>
      HistoryProbe(probe.date.toDate, probe.sp)
    }
    ColumnHistory("Estimate", doneColumn = false, estimates)
  }
}

case class GetColumnsHistory(sprintId: Int) extends NgModel

case class ColumnsHistory(startDate: Long, series: List[ColumnHistory])

case class ColumnHistory(name: String, doneColumn: Boolean, data: List[HistoryProbe])

case class HistoryProbe private(x: Long, y: Double)

object HistoryProbe {
  def apply(x: Date, y: BigDecimal): HistoryProbe = {
    new HistoryProbe(x.getTime, y.toString().toDouble)
  }
}
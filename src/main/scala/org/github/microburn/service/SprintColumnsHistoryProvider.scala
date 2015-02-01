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

import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.Box
import org.github.microburn.domain.actors.{GetStoryPointsHistory, SprintHistory}
import org.github.microburn.domain.{DateWithColumnsState, ProjectConfig}
import org.joda.time.DateTime

import scalaz.Scalaz._

class SprintColumnsHistoryProvider(projectActor: LiftActor, initialFetchToSprintStartAcceptableDelayMinutes: Int)(implicit config: ProjectConfig) {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._
  
  def columnsHistory(sprintId: String): LAFuture[Box[ColumnsHistory]] = {
    (projectActor ?? GetStoryPointsHistory(sprintId)).mapTo[Box[SprintHistory]].map { historyBox =>
      historyBox.map(extractColumnsHistory)
    }
  }

  private def extractColumnsHistory(history: SprintHistory): ColumnsHistory = {
    val toPrepend = computeToPrepend(history).toSeq
    val toAppend = computeToAppend(history)
    val fullHistory = toPrepend ++ history.columnStates ++ toAppend

    val baseIndexOnSum = DateWithColumnsState.constIndexOnSum(history.initialStoryPointsSum)
    val withBaseAdded = fullHistory.map(_.multiply(-1).plus(baseIndexOnSum))

    val columnsHistory = unzipByColumn(withBaseAdded)
    val startDate = new DateTime(history.sprintDetails.start)
    val endDate = new DateTime(history.sprintDetails.end)

    val estimate = computeEstimate(startDate, endDate, history.initialStoryPointsSum)

    val columns = estimate :: columnsHistory
    ColumnsHistory(startDate.getMillis, columns)
  }

  private def computeToPrepend(history: SprintHistory): Option[DateWithColumnsState] = {
    val initialDate = new DateTime(history.initialDate)
    val startDatePlusAcceptableDelay = new DateTime(history.sprintDetails.start).plusMinutes(initialFetchToSprintStartAcceptableDelayMinutes)
    val initialAfterStartPlusDelay = initialDate.isAfter(startDatePlusAcceptableDelay)
    initialAfterStartPlusDelay.option {
      DateWithColumnsState.zero(history.sprintDetails.start)
    }
  }

  private def computeToAppend(history: SprintHistory): Option[DateWithColumnsState] = {
    val nowOrSprintsEndForFinished = history.sprintDetails.finished.option(history.sprintDetails.end).getOrElse(new Date)
    val last = history.columnStates.last
    val lastAvailableBeforeNow = last.date.before(nowOrSprintsEndForFinished)
    lastAvailableBeforeNow.option {
      last.copy(date = nowOrSprintsEndForFinished)
    }
  }

  private def unzipByColumn(zipped: Seq[DateWithColumnsState]): List[ColumnHistory] = {
    val boardColumnsWithDroppedFirst = config.boardColumns.drop(1)
    boardColumnsWithDroppedFirst.map { column =>
      val storyPointsForColumn = zipped.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        HistoryProbe(allColumnsInfo.date.getTime, storyPoints.toFloat)
      }.toList
      ColumnHistory(column.name, storyPointsForColumn)
    }
  }

  private def computeEstimate(start: DateTime, end: DateTime, storyPointsSum: BigDecimal): ColumnHistory = {
    val estimates = EstimateComputer.estimatesBetween(start, end, storyPointsSum)
    ColumnHistory("Estimate", estimates)
  }
}

case class ColumnsHistory(startDate: Long, series: List[ColumnHistory])

case class ColumnHistory(name: String, data: List[HistoryProbe])

case class HistoryProbe(x: Long, y: Float)
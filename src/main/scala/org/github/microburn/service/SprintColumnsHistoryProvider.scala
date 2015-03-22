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
import org.github.microburn.domain._
import org.github.microburn.domain.actors.{GetStoryPointsHistory, ProjectActor}
import org.github.microburn.domain.history.{UserStoryChange, DateWithColumnsChanges, SprintHistory}
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

    val withBaseAdded = fullHistory.map(_ * -1 + history.sprintBase)

    val columnsHistory = unzipByColumn(withBaseAdded)
    val startDate = new DateTime(history.sprintDetails.start)
    val endDate = new DateTime(history.sprintDetails.end)

    val estimate = computeEstimate(startDate, endDate, history.sprintBase)

    val columns = estimate :: columnsHistory
    ColumnsHistory(startDate.getMillis, columns)
  }

  private def computeToAppend(history: SprintHistory): Option[DateWithColumnsChanges] = {
    val nowOrSprintsEndForFinished = history.sprintDetails.isFinished.option(history.sprintDetails.end).getOrElse(new Date)
    val last = history.columnStates.last
    val lastAvailableBeforeNow = last.date.before(nowOrSprintsEndForFinished)
    lastAvailableBeforeNow.option {
      last.copy(date = nowOrSprintsEndForFinished).withoutTaskChanges
    }
  }

  private def unzipByColumn(zipped: Seq[DateWithColumnsChanges]): List[ColumnHistory] = {
    config.nonBacklogColumns.map { column =>
      val storyPointsForColumn = zipped.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        val added = sortByPointsOrName(allColumnsInfo.addedForColumn(column.index))
        val removed = sortByPointsOrName(allColumnsInfo.removedForColumn(column.index))
        HistoryProbe(allColumnsInfo.date, storyPoints, ProbeDetails(added, removed))
      }
      ColumnHistory(column.name, column.isDoneColumn, storyPointsForColumn)
    }
  }

  private def sortByPointsOrName(changes: Seq[UserStoryChange]): Seq[UserStoryChange] = {
    val withSortedTechnical = changes.map(_.withSortedTechnicalBy(tech => (-tech.storyPoints, tech.name)))
    withSortedTechnical.sortBy(change => (-change.storyPointsSum, change.name))
  }

  private def computeEstimate(start: DateTime, end: DateTime, storyPointsSum: BigDecimal): ColumnHistory = {
    val estimates = EstimateComputer.estimatesBetween(start, end, storyPointsSum).map { probe =>
      HistoryProbe(probe.date.toDate, probe.sp, ProbeDetails.zero)
    }
    ColumnHistory("Estimate", doneColumn = false, estimates)
  }
}

case class GetColumnsHistory(sprintId: Int) extends NgModel

case class ColumnsHistory(startDate: Long, series: Seq[ColumnHistory])

case class ColumnHistory(name: String, doneColumn: Boolean, data: Seq[HistoryProbe])

case class HistoryProbe private(x: Long, y: Double, details: ProbeDetails)

case class ProbeDetails(addedPoints: Seq[UserStoryChange], removedPoints: Seq[UserStoryChange])

object ProbeDetails {
  def zero: ProbeDetails = ProbeDetails(Nil, Nil)
}

object HistoryProbe {
  def apply(x: Date, y: BigDecimal, details: ProbeDetails): HistoryProbe = {
    new HistoryProbe(x.getTime, y.toString().toDouble, details)
  }
}
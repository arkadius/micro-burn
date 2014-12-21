package org.github.jiraburn.service

import java.util.Date

import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.Box
import org.github.jiraburn.domain.actors.{GetStoryPointsHistory, SprintHistory}
import org.github.jiraburn.domain.{DateWithColumnsState, ProjectConfig, SprintDetails}
import org.joda.time.DateTime

import scalaz.Scalaz._

class SprintColumnsHistoryProvider(projectActor: LiftActor)(implicit config: ProjectConfig) {
  private final val INITIAL_TO_START_ACCEPTABLE_DELAY_HOURS = 2

  import org.github.jiraburn.util.concurrent.FutureEnrichments._
  import org.github.jiraburn.util.concurrent.LiftActorEnrichments._
  
  def columnsHistory(sprintId: String): LAFuture[Box[ColumnsHistoryWithDetails]] = {
    (projectActor ?? GetStoryPointsHistory(sprintId)).mapTo[Box[SprintHistory]].map { historyBox =>
      historyBox.map(extractColumnsHistory)
    }
  }
  
  private def extractColumnsHistory(history: SprintHistory): ColumnsHistoryWithDetails = {
    val toPrepend = computePrepend(history).toSeq
    val toAppend = computeToAppend(history)
    val fullHistory = toPrepend ++ history.columnStates ++ toAppend

    val baseIndexOnSum = DateWithColumnsState.constIndexOnSum(history.initialStoryPointsSum)
    val withBaseAdded = fullHistory.map(_.multiply(-1).plus(baseIndexOnSum))

    val columnsHistory = unzipByColumn(withBaseAdded)
    ColumnsHistoryWithDetails(history.sprintDetails, columnsHistory)
  }

  private def computePrepend(history: SprintHistory): Option[DateWithColumnsState] = {
    val initialDate = new DateTime(history.initialDate)
    val startDatePlusAcceptableDelay = new DateTime(history.sprintDetails.start).plusHours(INITIAL_TO_START_ACCEPTABLE_DELAY_HOURS)
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

  private def unzipByColumn(zipped: Seq[DateWithColumnsState]): List[ColumnWithStoryPointsHistory] = {
    val boardColumnsWithDroppedFirst = config.boardColumns.drop(1)
    boardColumnsWithDroppedFirst.map { column =>
      val storyPointsForColumn = zipped.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        DateWithStoryPointsForSingleColumn(allColumnsInfo.date, storyPoints)
      }.toList
      ColumnWithStoryPointsHistory(column.name, column.color, storyPointsForColumn)
    }
  }
}

case class ColumnsHistoryWithDetails(detail: SprintDetails, series: List[ColumnWithStoryPointsHistory])

case class ColumnWithStoryPointsHistory(name: String, color: String, data: List[DateWithStoryPointsForSingleColumn])

case class DateWithStoryPointsForSingleColumn(x: Long, y: Int)

object DateWithStoryPointsForSingleColumn {
  def apply(date: Date, storyPoints: Int): DateWithStoryPointsForSingleColumn =
    DateWithStoryPointsForSingleColumn(toSeconds(date), storyPoints)

  private def toSeconds(date: Date): Long = {
    date.getTime / 1000
  }
}
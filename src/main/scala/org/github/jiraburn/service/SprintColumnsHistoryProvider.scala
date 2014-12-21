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
    val now = new Date
    val (toPrepend, base) = computePrependAndBase(history)
    val prependedAndChanges = toPrepend ++ history.changes
    val toAppend = computeToAppend(history, now, prependedAndChanges)
    val fullHistory = prependedAndChanges ++ toAppend

    val withBaseAdded = fullHistory.map(_.plus(base.indexOnSum))

    val columnsHistory = unzipByColumn(withBaseAdded)
    ColumnsHistoryWithDetails(history.sprintDetails, columnsHistory)
  }

  private def computePrependAndBase(history: SprintHistory): (Seq[DateWithColumnsState], DateWithColumnsState) = {
    val initialDate = new DateTime(history.initialState.date)
    val startDatePlusAcceptableDelay = new DateTime(history.sprintDetails.start).plusHours(INITIAL_TO_START_ACCEPTABLE_DELAY_HOURS)
    val initialBeforeStartPlusDelay = initialDate.isBefore(startDatePlusAcceptableDelay)
    if (initialBeforeStartPlusDelay) {
      (Seq(DateWithColumnsState.zero(history.initialState.date)), history.initialState)
    } else {
      val baseFromStoryPointSum = DateWithColumnsState.const(history.initialStoryPointsSum)(history.sprintDetails.start)
      val toPrepend = Seq(
        DateWithColumnsState.zero(history.sprintDetails.start),
        history.initialState.multiply(-1)
      )
      (toPrepend, baseFromStoryPointSum)
    }
  }

  private def computeToAppend(history: SprintHistory, now: Date, prependedAndChanges: Seq[DateWithColumnsState]): Option[DateWithColumnsState] = {
    val nowOrSprintsEndForFinished = history.sprintDetails.finished.option(history.sprintDetails.end).getOrElse(now)
    val last = prependedAndChanges.last
    val lastAvailableBeforeNow = last.date.before(nowOrSprintsEndForFinished)
    if (lastAvailableBeforeNow) {
      Some(last.copy(date = nowOrSprintsEndForFinished))
    } else {
      None
    }
  }

  private def unzipByColumn(zipped: Seq[DateWithColumnsState]): List[ColumnWithStoryPointsHistory] = {
    val boardColumnsWithDroppedFirst = config.boardColumns.drop(1)
    boardColumnsWithDroppedFirst.map { column =>
      val storyPointsForColumn = zipped.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        DateWithStoryPointsForSingleColumn(allColumnsInfo.date.getTime, storyPoints)
      }.toList
      ColumnWithStoryPointsHistory(column.name, storyPointsForColumn)
    }
  }
}

case class ColumnsHistoryWithDetails(detail: SprintDetails, columnsHistory: List[ColumnWithStoryPointsHistory])

case class ColumnWithStoryPointsHistory(name: String, storyPointsChanges: List[DateWithStoryPointsForSingleColumn])

case class DateWithStoryPointsForSingleColumn(x: Long, y: Int)
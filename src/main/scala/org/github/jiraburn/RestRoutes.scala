package org.github.jiraburn

import java.util.Date

import net.liftweb.actor.LiftActor
import net.liftweb.common.Box
import net.liftweb.http.rest.RestHelper
import org.github.jiraburn.domain.{SprintDetails, DateWithStoryPoints, ProjectConfig}
import org.github.jiraburn.domain.actors.{GetStoryPointsHistory, SprintHistory}

import scalaz._
import Scalaz._

class RestRoutes(projectActor: LiftActor)(implicit config: ProjectConfig) extends RestHelper {

  import net.liftweb.json.Extraction._
  import org.github.jiraburn.util.concurrent.FutureEnrichments._
  import org.github.jiraburn.util.concurrent.LiftActorEnrichments._

  serve {
    case JsonGet("history" ::Nil, req) =>
      val sprintId = req.param("sprintId").openOr(throw new IllegalArgumentException("sprintId must be provided"))
      (projectActor ?? GetStoryPointsHistory(sprintId)).mapTo[Box[SprintHistory]].map { historyBox =>
        historyBox.map { history =>
          val columnsHistory = extractColumnsHistory(history, new Date)
          decompose(ColumnsHistoryWithDetails(history.sprintDetails, columnsHistory))
        }
      }
  }

  private def extractColumnsHistory(history: SprintHistory, now: Date)
                                   (implicit config: ProjectConfig): List[ColumnWithStoryPointsHistory] = {
    val firstRealChangeNotBeforeSprintStart = !history.changes.headOption.exists(!_.date.after(history.sprintDetails.start))
    val toPrepend = firstRealChangeNotBeforeSprintStart.option(DateWithStoryPoints.zero(history.sprintDetails.start))

    val nowOrSprintsEndForFinished = history.sprintDetails.finished.option(history.sprintDetails.end).getOrElse(now)
    val lastRealChangeNotAfterNow = !history.changes.lastOption.exists(!_.date.before(nowOrSprintsEndForFinished))
    val toAppend = lastRealChangeNotAfterNow.option {
      history.changes.lastOption.map { last =>
        last.copy(date = nowOrSprintsEndForFinished)
      } getOrElse DateWithStoryPoints.zero(nowOrSprintsEndForFinished)
    }

    val fullHistory = toPrepend ++ history.changes ++ toAppend
    val withBaseAdded = fullHistory.map(_.plus(history.initialStoryPoints))
    val boarColumnsWithDroppedTODO = config.boardColumns.drop(1)
    boarColumnsWithDroppedTODO.map { column =>
      val storyPointsForColumn = withBaseAdded.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        DateWithStoryPointsForSingleColumn(allColumnsInfo.date, storyPoints)
      }.toList
      ColumnWithStoryPointsHistory(column.name, storyPointsForColumn)
    }
  }
}

case class ColumnsHistoryWithDetails(detail: SprintDetails, columnsHistory: List[ColumnWithStoryPointsHistory])

case class ColumnWithStoryPointsHistory(name: String, storyPointsChanges: List[DateWithStoryPointsForSingleColumn])

case class DateWithStoryPointsForSingleColumn(date: Date, storyPoints: Int)


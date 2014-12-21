package org.github.jiraburn

import java.util.Date

import net.liftweb.actor.LiftActor
import net.liftweb.common.Box
import net.liftweb.http.rest.RestHelper
import org.github.jiraburn.domain.actors.{GetStoryPointsHistory, SprintHistory}

class RestRoutes(projectActor: LiftActor) extends RestHelper {

  import net.liftweb.json.Extraction._
  import org.github.jiraburn.util.concurrent.FutureEnrichments._
  import org.github.jiraburn.util.concurrent.LiftActorEnrichments._

  serve {
    case JsonGet("history" ::Nil, req) =>
      val sprintId = req.param("sprintId").openOr(throw new IllegalArgumentException("sprintId must be provided"))
      (projectActor ?? GetStoryPointsHistory(sprintId)).mapTo[Box[SprintHistory]].map { historyBox =>
        historyBox.map { history =>
          decompose(history.columnsHistoryFor(new Date).toList)
        }
      }
  }

}

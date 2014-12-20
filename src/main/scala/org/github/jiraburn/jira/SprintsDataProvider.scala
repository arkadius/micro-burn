package org.github.jiraburn.jira

import java.text.SimpleDateFormat

import dispatch._
import net.liftweb.actor.LAFuture
import org.github.jiraburn.domain.SprintDetails
import org.json4s._

class SprintsDataProvider(config: JiraConfig) {
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.github.jiraburn.util.concurrent.FutureEnrichments._

  def allSprintIds: LAFuture[Seq[Long]] = {
    val url = config.greenhopperUrl / "sprints" / config.rapidViewId
    Http(url OK as.json4s.Json).toLiftFuture.map { jv =>
      (jv \ "sprints" \\ "id").children.collect {
        case JInt(value) => value.toLong
      }
    }
  }

  def sprintDetails(sprintId: String): LAFuture[SprintDetails] = {
    val url = config.greenhopperUrl / "rapid" / "charts" / "sprintreport" <<? Map(
      "rapidViewId" -> config.rapidViewId.toString,
      "sprintId" -> sprintId
    )
    Http(url OK as.json4s.Json).toLiftFuture.map { jv =>
      val JString(name) = jv \ "sprint" \ "name"
      val JBool(closed) = jv \ "sprint" \ "closed"
      val JString(startDate) = jv \ "sprint" \ "startDate"
      val JString(endDate) = jv \ "sprint" \ "endDate"
      SprintDetails(name, dateFormat.parse(startDate), dateFormat.parse(endDate), !closed)
    }
  }

  private def dateFormat = new SimpleDateFormat("dd/MMM/yy HH:mm")

}

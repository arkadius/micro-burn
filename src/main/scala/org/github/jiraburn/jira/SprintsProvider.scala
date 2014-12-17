package org.github.jiraburn.jira

import java.text.SimpleDateFormat

import dispatch._
import org.github.jiraburn.domain.SprintDetails
import org.json4s._

class SprintsProvider(config: JiraConfig) {
  import scala.concurrent.ExecutionContext.Implicits.global

  def allSprintIds: Future[List[Long]] = {
    val url = config.greenhopperUrl / "sprints" / config.rapidViewId
    Http(url OK as.json4s.Json) map { jv =>
      (jv \ "sprints" \\ "id").children.collect {
        case JInt(value) => value.toLong
      }
    }
  }

  def sprintDetails(sprintId: Long): Future[SprintDetails] = {
    val url = config.greenhopperUrl / "rapid" / "charts" / "sprintreport" <<? Map(
      "rapidViewId" -> config.rapidViewId.toString,
      "sprintId" -> sprintId.toString
    )
    Http(url OK as.json4s.Json) map { jv =>
      val JString(name) = jv \ "sprint" \ "name"
      val JBool(closed) = jv \ "sprint" \ "closed"
      val JString(startDate) = jv \ "sprint" \ "startDate"
      val JString(endDate) = jv \ "sprint" \ "endDate"
      SprintDetails(name, dateFormat.parse(startDate), dateFormat.parse(endDate), !closed)
    }
  }

  private def dateFormat = new SimpleDateFormat("dd/MMM/yy HH:mm")

}

package org.github.jiraburn.jira

import dispatch._
import org.json4s._

class SprintsProvider(config: JiraConfig) {
  import scala.concurrent.ExecutionContext.Implicits.global

  def allSprints = {
    val url = config.greenhopperUrl / "sprints" / config.rapidViewId
    println(url)

    Http(url OK as.json4s.Json) map { jv =>
      jv \\ ""
    }
  }

}

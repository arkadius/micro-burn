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
package org.github.microburn.integration.jira

import java.text.SimpleDateFormat
import java.util.Locale

import dispatch._
import net.liftweb.actor.LAFuture
import org.github.microburn.domain.MajorSprintDetails
import org.github.microburn.integration.support.scrum.SprintsDataProvider
import org.json4s._

class JiraSprintsDataProvider(config: JiraConfig, locale: java.util.Locale = Locale.getDefault(Locale.Category.FORMAT)) extends SprintsDataProvider {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def allSprintIds: LAFuture[Seq[Int]] = {
    val url = config.greenhopperUrl / "sprints" / config.rapidViewId
    Http(url OK as.json4s.Json).toLiftFuture.map { jv =>
      (jv \ "sprints" \\ "id").children.collect {
        case JInt(value) => value.toInt
      }
    }
  }

  override def sprintDetails(sprintId: Int): LAFuture[MajorSprintDetails] = {
    val url = config.greenhopperUrl / "rapid" / "charts" / "sprintreport" <<? Map(
      "rapidViewId" -> config.rapidViewId.toString,
      "sprintId" -> sprintId.toString
    )
    Http(url OK as.json4s.Json).toLiftFuture.map { jv =>
      val JString(name) = jv \ "sprint" \ "name"
      val JBool(closed) = jv \ "sprint" \ "closed"
      val JString(startDate) = jv \ "sprint" \ "startDate"
      val JString(endDate) = jv \ "sprint" \ "endDate"
      MajorSprintDetails(name, dateFormat.parse(startDate), dateFormat.parse(endDate), !closed)
    }
  }

  private def dateFormat = new SimpleDateFormat("dd/MMM/yy HH:mm", locale)
}
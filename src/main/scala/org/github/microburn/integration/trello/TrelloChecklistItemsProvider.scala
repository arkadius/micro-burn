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
package org.github.microburn.integration.trello

import dispatch.Http
import net.liftweb.actor.LAFuture
import org.github.microburn.domain.TechnicalTask
import org.github.microburn.util.dispatch._
import org.json4s.JsonAST.JString

class TrelloChecklistItemsProvider(config: TrelloConfig) {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  import scala.concurrent.ExecutionContext.Implicits.global

  def checklistItems(cardId: String, cardStatus: String): LAFuture[Seq[TechnicalTask]] = {
    val url = config.prepareUrl(_ / "checklist" / cardId <<? Map(
      "fields" -> "checkItems"
    ))
    Http(url OK asJsonWithUtf8).toLiftFuture.map { jv =>
      (jv \ "checkItems").children.map { item =>
        val JString(state) = item \ "state"
        val status = if (state == "complete")
          "completed" // FIXME: status
        else
          cardStatus
        val JString(id) = item \ "id"
        val JString(StoryPointsFromName(optionalSp, name)) = item \ "name"
        TechnicalTask(id, name, optionalSp, status)
      }
    }
  }

}
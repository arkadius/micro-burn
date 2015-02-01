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

import java.util.Date

import dispatch.{Http, Req}
import net.liftweb.actor.LAFuture
import org.github.microburn.util.dispatch._
import org.github.microburn.util.date._
import org.json4s.JsonAST._

class TrelloCardsProvider(config: TrelloConfig) {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  import scala.concurrent.ExecutionContext.Implicits.global

  def cards: LAFuture[Seq[Card]] = {
    fetchCardsWithChecklistItems(_ / "board" / config.boardId / "cards" / "all")
  }

  private def fetchCardsWithChecklistItems(urlFromApi: Req => Req): LAFuture[Seq[Card]] = {
    val url = config.prepareUrl { apiUrl =>
      urlFromApi(apiUrl) <<? Map(
        "fields" -> "id,name,idList,closed,dateLastActivity",
        "checklists" -> "all"
      )
    }
    fetchCards(url)
  }

  private def fetchCards(url: Req): LAFuture[Seq[Card]] = {
    Http(url OK asJsonWithUtf8).toLiftFuture.map { jv =>
      jv.children.map { item =>
        val JString(id) = item \ "id"
        val JString(StoryPointsFromName(optionalSp, name)) = item \ "name"
        val JString(columnId) = item \ "idList"
        val JBool(closed) = item \ "closed"
        val JString(dateStr) = item \ "dateLastActivity"
        val dateLastActivity = utcDateFormat.parse(dateStr)
        val checkListItems = ChecklistExtractor.extract(item \ "checklists")
        Card(
          id = id,
          name = name,
          optionalSp = optionalSp,
          columnId = columnId,
          closed = closed,
          checkListItems = checkListItems,
          dateLastActivity = dateLastActivity
        )
      }
    }
  }
}

case class Card(id: String,
                name: String,
                optionalSp: Option[BigDecimal],
                columnId: String,
                closed: Boolean,
                checkListItems: Seq[ChecklistItem],
                dateLastActivity: Date)
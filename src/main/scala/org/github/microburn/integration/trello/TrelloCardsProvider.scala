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

import dispatch.{Http, Req}
import net.liftweb.actor.LAFuture
import org.github.microburn.util.dispatch._
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
        "fields" -> "id,name,idList,closed",
        "checklists" -> "all"
      )
    }
    fetchCards(url)
  }

  private def fetchCards(url: Req): LAFuture[Seq[Card]] = {
    Http(url OK asJsonWithUtf8).toLiftFuture.map { jv =>
      jv.children.map { item =>
        val JString(id) = item \ "id"
        val JString(name) = item \ "name"
        val JString(columnId) = item \ "idList"
        val checkListItems = ChecklistExtractor.extract(item \ "checklists")
        val JBool(closed) = item \ "closed"
        Card(
          id = id,
          name = name,
          columnId = columnId,
          checklistItems = checkListItems,
          closed = closed
        )
      }
    }
  }
}

case class Card(id: String,
                protected val name: String,
                columnId: String,
                checklistItems: Seq[ChecklistItem],
                closed: Boolean) extends TrelloTask
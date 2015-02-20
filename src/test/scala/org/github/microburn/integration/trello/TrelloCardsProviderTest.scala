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

import akka.actor.ActorSystem
import org.github.microburn.TestConfig
import org.github.microburn.integration.RestIntegrationTest
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}
import spray.routing._

class TrelloCardsProviderTest extends FlatSpec with RestIntegrationTest with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import scala.concurrent.duration._

  override protected def route: Route = TrelloCardsProviderTest.route

  it should "fetch cards with inner checklist items" in {
    val config = TestConfig.trelloConfigWithDefaultsFallback(fromFile = false)
    val provider = new TrelloCardsProvider(TrelloConfig(config.getConfig("trello")))

    val result = provider.cards.await(5.seconds)

    result should have length 3
    result(0) shouldEqual Card(
      id = "closedCardId",
      name = "Closed card name",
      columnId = "doneId",
      checklistItems = List(
        ChecklistItem("completeItemId", "Complete item name", closed = true)
      )
    )
    result(1) shouldEqual Card(
      id = "openedCardWithMultipleChecklistsId",
      name = "(1.5) Opened card with multiple checklists name",
      columnId = "todoId",
      checklistItems = List(
        ChecklistItem("incompleteItemId", "(0.5) Incomplete item name", closed = false),
        ChecklistItem("incompleteItem2Id", "Incomplete item 2 name", closed = false),
        ChecklistItem("incompleteItemWithNonAsciiCharsId", "Incomplete item with ąż name", closed = false)
      )
    )
    result(2) shouldEqual Card(
      id = "openedCardWithoutChecklists",
      name = "Opened card with checklist name",
      columnId = "backlogId",
      checklistItems = Nil
    )
  }
}

object TrelloCardsProviderTest extends Directives {
  def route(implicit system: ActorSystem, routeSettings: RoutingSettings): Route = {
    val trelloUrl = "trello" / "1"
    path(trelloUrl / "board" / Segment / "cards") { boardId =>
      get {
        getFromFile("src/test/resources/trello/cards.json")
      }
    }
  }
}
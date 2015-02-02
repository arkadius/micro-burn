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

import akka.actor.ActorSystem
import org.github.microburn.TestConfig
import org.github.microburn.integration.RestIntegrationTest
import org.joda.time.{DateTimeZone, DateTime}
import org.scalatest.{FlatSpec, Matchers}
import spray.routing._

class TrelloCardsProviderTest extends FlatSpec with RestIntegrationTest with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  import scala.concurrent.duration._

  override protected def route: Route = TrelloCardsProviderTest.route

  it should "get checklist" in {
    val config = TestConfig.trelloConfigWithDefaultsFallback(fromFile = false)
    val provider = new TrelloCardsProvider(TrelloConfig(config.getConfig("trello")))

    val result = provider.cards.await(5.seconds)

    result should have length 3
    result(0) shouldEqual Card(
      id = "closedCardId",
      name = "Closed card name",
      optionalSp = None,
      columnId = "doneId",
      closed = true,
      checkListItems = List(
        ChecklistItem("completeItemId", "Complete item name", None, completedStatus = true)
      ),
      dateLastActivity = new DateTime(2014, 12, 14, 0, 0, DateTimeZone.UTC).toDate
    )
    result(1) shouldEqual Card(
      id = "openedCardWithMultipleChecklistsId",
      name = "Opened card with multiple checklists name",
      optionalSp = Some(BigDecimal("1.5")),
      columnId = "todoId",
      closed = false,
      checkListItems = List(
        ChecklistItem("incompleteItemId", "Incomplete item name", Some(BigDecimal("0.5")), completedStatus = false),
        ChecklistItem("incompleteItem2Id", "Incomplete item 2 name", None, completedStatus = false),
        ChecklistItem("incompleteItemWithNonAsciiCharsId", "Incomplete item with ąż name", None, completedStatus = false)
      ),dateLastActivity = new DateTime(2014, 12, 15, 0, 0, DateTimeZone.UTC).toDate
    )
    result(2) shouldEqual Card(
      id = "openedCardWithoutChecklists",
      name = "Opened card with checklist name",
      optionalSp = None,
      columnId = "backlogId",
      closed = false,
      checkListItems = Nil,
      dateLastActivity = new DateTime(2014, 12, 16, 0, 0, DateTimeZone.UTC).toDate
    )
  }
}

object TrelloCardsProviderTest extends Directives {
  def route(implicit system: ActorSystem, routeSettings: RoutingSettings): Route = {
    val trelloUrl = "trello" / "1"
    path(trelloUrl / "board" / Segment / "cards" / "all") { boardId =>
      get {
        getFromFile("src/test/resources/trello/cards.json")
      }
    }
  }
}
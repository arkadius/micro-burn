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
import org.github.microburn.domain.TechnicalTask
import org.github.microburn.integration.RestIntegrationTest
import org.scalatest.{FlatSpec, Matchers}
import spray.routing._

class TrelloChecklistItemsProviderTest extends FlatSpec with RestIntegrationTest with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  import scala.concurrent.duration._

  override protected def route: Route = TrelloChecklistItemsProviderTest.route

  it should "get checklist" in {
    val config = TestConfig.trelloConfigWithDefaultsFallback(fromFile = false)
    val provider = new TrelloChecklistItemsProvider(TrelloConfig(config.getConfig("trello")))

    val cardStatus = "fooStatus"
    val result = provider.checklistItems("54abf892895ce7e685938f0c", cardStatus).await(5.seconds)

    result should have length 4
    result(0) shouldEqual TechnicalTask("incompleteItemId", "Incomplete item name", None, cardStatus)
    result(1) shouldEqual TechnicalTask("completeItemId", "Complete item name", None, "completed") // FIXME: status
    result(2) shouldEqual TechnicalTask("incompleteItemWithUtf8CharsId", "Incomplete item name with utf8 chars: ąż", None, cardStatus)
    result(3) shouldEqual TechnicalTask("incompleteItemWithStoryPointsId", "Incomplete item with story points name", Some(22), cardStatus)
  }

}

object TrelloChecklistItemsProviderTest extends Directives {
  def route(implicit system: ActorSystem, routeSettings: RoutingSettings): Route = {
    val trelloUrl = "trello" / "1"
    path(trelloUrl / "checklist" / Segment) { checklistId =>
      get {
        getFromFile("src/test/resources/trello/checklist.json")
      }
    }
  }
}
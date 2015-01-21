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

import java.util.Locale

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.github.microburn.ConfigUtils
import org.github.microburn.domain.SprintDetails
import org.github.microburn.integration.RestIntegrationTest
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{Directives, Route, RoutingSettings}

import scala.concurrent.duration._

class JiraSprintsDataProviderTest extends FlatSpec with RestIntegrationTest with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  override protected val route: Route = JiraSprintsDataProviderTest.route

  it should "get sprints ids" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigUtils.withToDefaultsFallback)
    val config = ConfigUtils.withToDefaultsFallback
    val jiraConfig = JiraConfig(config.getConfig("jira"))
    val provider = new JiraSprintsDataProvider(jiraConfig, Locale.ENGLISH)
    val result = provider.allSprintIds.await(5 seconds)
    result shouldEqual Seq(21, 22)
  }

  it should "get sprint details" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigUtils.withToDefaultsFallback)
    val config = ConfigUtils.withToDefaultsFallback
    val jiraConfig = JiraConfig(config.getConfig("jira"))
    val provider = new JiraSprintsDataProvider(jiraConfig, Locale.ENGLISH)
    val result = provider.sprintDetails("21").await(5 seconds)
    println("result: " + result)
    result shouldEqual SprintDetails(
      "Sprint 1",
      new DateTime(2013, 11, 20, 14, 30).toDate, // 20/nov/13 14:30
      new DateTime(2013, 11, 27, 14, 30).toDate, // 27/nov/13 14:30
      isActive = false
    )
  }
}

object JiraSprintsDataProviderTest extends Directives {
  def route(implicit system: ActorSystem, routeSettings: RoutingSettings): Route = {
    val greenhooper = "jira" / "rest" / "greenhopper" / "1.0"
    path(greenhooper / "sprints" / IntNumber) { projectId =>
      get {
        getFromFile("src/test/resources/jira/allSprints.json")
      }
    } ~
    path(greenhooper / "rapid" / "charts" / "sprintreport") {
      get {
        getFromFile("src/test/resources/jira/sprintDetails.json")
      }
    }
  }
}
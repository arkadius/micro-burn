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

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.github.microburn.TestConfig
import org.github.microburn.domain.{SpecifiedStatus, TechnicalTask, UserStory}
import org.github.microburn.integration.RestIntegrationTest
import org.scalatest.{FlatSpec, Matchers}
import spray.routing._

import scala.concurrent.duration._

class JiraTasksDataProviderTest extends FlatSpec with RestIntegrationTest with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  override protected val route: Route = JiraTasksDataProviderTest.route

  it should "get user stories" in {
    val config = TestConfig.jiraConfigWithDefaultsFallback()
    val jiraConfig = JiraConfig(config.getConfig("jira"))
    val provider = new JiraTasksDataProvider(jiraConfig)
    val result = provider.userStories("fooSprintId").await(5.seconds)
    println(result)

    result shouldEqual Seq(
      UserStory("FOO-635","Bar user story", None, IndexedSeq.empty, SpecifiedStatus("1")),
      UserStory("FOO-452","Foo user story", Some(5), IndexedSeq(
        TechnicalTask("FOO-631","Foo subtask", Some(2) , SpecifiedStatus("1"))
      ), SpecifiedStatus("3"))
    )
  }

}

object JiraTasksDataProviderTest extends Directives {
  def route(implicit system: ActorSystem, routeSettings: RoutingSettings): Route = {
    val jira = "jira" / "rest" / "api" / "latest"
    path(jira / "search") {
      get {
        getFromFile("src/test/resources/jira/tasks.json")
      }
    }
  }
}
package org.github.microburn.integration.jira

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.github.microburn.ConfigUtils
import org.github.microburn.domain.{TechnicalTask, UserStory}
import org.github.microburn.integration.RestIntegrationTest
import org.scalatest.{FlatSpec, Matchers}
import spray.routing._

import scala.concurrent.duration._

class JiraTasksDataProviderTest extends FlatSpec with RestIntegrationTest with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  override protected val route: Route = JiraTasksDataProviderTest.route

  it should "get user stories" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigUtils.withToDefaultsFallback)
    val config = ConfigUtils.withToDefaultsFallback
    val jiraConfig = JiraConfig(config.getConfig("jira"))
    val provider = new JiraTasksDataProvider(jiraConfig)
    val result = provider.userStories("fooSprintId").await(5 seconds)
    println(result)

    result shouldEqual Seq(
      UserStory("FOO-635","Bar user story", None, IndexedSeq.empty, 1),
      UserStory("FOO-452","Foo user story", Some(5), IndexedSeq(
        TechnicalTask("FOO-631","Foo subtask", Some(2) , 1)
      ), 3)
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
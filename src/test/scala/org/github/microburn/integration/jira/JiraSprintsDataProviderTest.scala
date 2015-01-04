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

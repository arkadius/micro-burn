package org.github.jiraburn.jira

import com.typesafe.config.ConfigFactory
import org.github.jiraburn.domain.SprintDetails
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{Directives, Route}

import scala.concurrent.duration._

class SprintsDataProviderTest extends FlatSpec with RestIntegrationTest with Directives with Matchers {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._

  override protected def route: Route = {
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

  it should "get sprints ids" in {
//    val config = ConfigFactory.parseFile(new File("secret.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val jiraConfig = JiraConfig(config)
    val provider = new SprintsDataProvider(jiraConfig)
    val result = provider.allSprintIds.await(5 seconds)
    result shouldEqual Seq(21, 22)
  }

  it should "get sprint details" in {
//    val config = ConfigFactory.parseFile(new File("secret.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val jiraConfig = JiraConfig(config)
    val provider = new SprintsDataProvider(jiraConfig)
    val result = provider.sprintDetails("21").await(5 seconds)
    println("result: " + result)
    result shouldEqual SprintDetails(
      "Sprint 1",
      new DateTime(2013, 11, 20, 14, 30).toDate, // 20/lis/13 14:30
      new DateTime(2013, 11, 27, 14, 30).toDate, // 27/lis/13 14:30
      isActive = false
    )
  }



}

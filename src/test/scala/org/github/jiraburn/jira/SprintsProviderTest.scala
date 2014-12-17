package org.github.jiraburn.jira

import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec
import spray.routing.{Directives, Route}

import scala.concurrent.Await
import scala.concurrent.duration._

class SprintsProviderTest extends FlatSpec with RestIntegrationTest with Directives {

  override protected def route: Route =
    pathPrefix("jira") {
      //        path("/jira/rest/greenhopper/1.0/sprints/56") {
      get {
        getFromFile("src/test/resources/jira/allSprints.json")
      }
    }

  it should "get sprints" in {
//    val config = ConfigFactory.parseFile(new File("secret.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val jiraConfig = JiraConfig(config)
    val provider = new SprintsProvider(jiraConfig)
    val result = Await.result(provider.allSprints, 5 seconds)
    println(result)
  }

}

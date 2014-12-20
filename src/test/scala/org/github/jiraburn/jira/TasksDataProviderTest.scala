package org.github.jiraburn.jira

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.jiraburn.domain.{TechnicalTask, UserStory}
import org.scalatest.{Matchers, FlatSpec}
import spray.routing._
import scala.concurrent.duration._

class TasksDataProviderTest extends FlatSpec with RestIntegrationTest with Directives with Matchers {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._

  override protected def route: Route = {
    val jira = "jira" / "rest" / "api" / "latest"
    path(jira / "search") {
      get {
        getFromFile("src/test/resources/jira/tasks.json")
      }
    }
  }

  it should "get user stories" in {
//    val config = ConfigFactory.parseFile(new File("secret.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val jiraConfig = JiraConfig(config)
    val provider = new TasksDataProvider(jiraConfig)
    val result = provider.userStories("fooSprintId").await(5 seconds)
    println(result)

    result shouldEqual Seq(
      UserStory("FOO-635","Bar user story", None, Nil, 1),
      UserStory("FOO-452","Foo user story", Some(5), List(
        TechnicalTask("FOO-631","Foo subtask", Some(2) , 1)
      ), 3)
    )
  }

}

package org.github.jiraburn.service

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.MockLiftActor
import org.github.jiraburn.domain.ProjectConfig
import org.github.jiraburn.domain.actors.ProjectActor
import org.github.jiraburn.jira._
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{Directives, Route}

import scala.concurrent.duration._
import scala.reflect.io.Path

class ProjectUpdaterTest extends FlatSpec with RestIntegrationTest with Directives with Matchers {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._

  override def route: Route = SprintsDataProviderTest.route ~ TasksDataProviderTest.route

  it should "fetch inital project state" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val projectRoot = new File("target/sprints")
    Path(projectRoot).deleteRecursively()
    val projectActor = new ProjectActor(projectRoot, ProjectConfig(config), new MockLiftActor)
    val jiraConfig = JiraConfig(config)
    val updater = new ProjectUpdater(projectActor, new SprintsDataProvider(jiraConfig), new TasksDataProvider(jiraConfig))
    updater.updateProject().await(10 seconds)
  }

}
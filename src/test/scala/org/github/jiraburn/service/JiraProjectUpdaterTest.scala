package org.github.jiraburn.service

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.MockLiftActor
import org.github.jiraburn.domain.ProjectConfig
import org.github.jiraburn.domain.actors.ProjectActor
import org.github.jiraburn.integration._
import org.github.jiraburn.integration.jira._
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{Directives, Route}

import scala.concurrent.duration._
import scala.reflect.io.Path

class JiraProjectUpdaterTest extends FlatSpec with RestIntegrationTest with Directives with Matchers {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._

  override def route: Route = JiraSprintsDataProviderTest.route ~ JiraTasksDataProviderTest.route

  it should "fetch inital project state" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val projectRoot = new File(config.getString("data.project.root"))
    Path(projectRoot).deleteRecursively()
    val projectActor = new ProjectActor(projectRoot, ProjectConfig(config), new MockLiftActor)
    val jiraConfig = JiraConfig(config)
    val providers = IntegrationProviders(new JiraSprintsDataProvider(jiraConfig), new JiraTasksDataProvider(jiraConfig))
    val updater = new ProjectUpdater(projectActor, providers, updatePeriodSeconds = 123)
    updater.updateProject().await(10 seconds)
  }

}
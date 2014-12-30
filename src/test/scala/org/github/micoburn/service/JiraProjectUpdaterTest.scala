package org.github.micoburn.service

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.MockLiftActor
import org.github.micoburn.ConfigUtils
import org.github.micoburn.domain.ProjectConfig
import org.github.micoburn.domain.actors.ProjectActor
import org.github.micoburn.integration._
import org.github.micoburn.integration.jira._
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{Directives, Route}

import scala.concurrent.duration._
import scala.reflect.io.Path

class JiraProjectUpdaterTest extends FlatSpec with RestIntegrationTest with Directives with Matchers {
  import org.github.micoburn.util.concurrent.FutureEnrichments._

  override def route: Route = JiraSprintsDataProviderTest.route ~ JiraTasksDataProviderTest.route

  it should "fetch inital project state" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigUtils.withToDefaultsFallback)
    val config = ConfigUtils.withToDefaultsFallback
    val projectConfig = ProjectConfig(config)
    Path(projectConfig.dataRoot).deleteRecursively()
    val projectActor = new ProjectActor(projectConfig, new MockLiftActor)
    val jiraConfig = JiraConfig(config.getConfig("jira"))
    val providers = IntegrationProviders(new JiraSprintsDataProvider(jiraConfig), new JiraTasksDataProvider(jiraConfig))
    val updater = new ProjectUpdater(projectActor, providers, updatePeriodSeconds = 123)
    updater.updateProject().await(10 seconds)
  }

}
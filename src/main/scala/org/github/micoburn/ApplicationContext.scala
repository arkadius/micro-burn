package org.github.micoburn

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import net.liftweb.actor.MockLiftActor
import org.github.micoburn.domain.ProjectConfig
import org.github.micoburn.domain.actors.ProjectActor
import org.github.micoburn.integration.jira.{JiraConfig, JiraSprintsDataProvider, JiraTasksDataProvider}
import org.github.micoburn.service.{ProjectUpdater, IntegrationProviders, SprintColumnsHistoryProvider}

class ApplicationContext private(val updater: ProjectUpdater,
                                 val columnsHistoryProvider: SprintColumnsHistoryProvider,
                                 val jettyPort: Int)

object ApplicationContext {
  def apply(): ApplicationContext = context

  private lazy val context = {
    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.parseResources("defaults.conf"))
    val projectRoot = new File(config.getString("data.project.root"))
    implicit val projectConfig = ProjectConfig(config)
    val projectActor = new ProjectActor(projectRoot, projectConfig, new MockLiftActor)

    val jettyPort = config.getInt("jetty.port")
    val updatePeriodSeconds = config.getInt("integration.updatePeriodSeconds")
    val initialFetchToSprintStartAcceptableDelayMinutes = config.getInt("history.initialFetchToSprintStartAcceptableDelayMinutes")

    val providers = firstConfiguredProvider.applyOrElse(config, throw new IllegalArgumentException("You must define integration tool"))
    val updater = new ProjectUpdater(projectActor, providers, updatePeriodSeconds)

    val columnsHistoryProvider = new SprintColumnsHistoryProvider(projectActor, initialFetchToSprintStartAcceptableDelayMinutes)

    new ApplicationContext(
      updater                 = updater,
      columnsHistoryProvider  = columnsHistoryProvider,
      jettyPort               = jettyPort)
  }

  private def firstConfiguredProvider: PartialFunction[Config, IntegrationProviders] =
    supportedProviders.foldLeft(PartialFunction.empty[Config, IntegrationProviders]) { case (acc, tryProvider) =>
      acc orElse tryProvider
    }

  private val tryJiraProvider: PartialFunction[Config, IntegrationProviders] = {
    case config if config.hasPath("jira") =>
      val jiraConfig = JiraConfig(config)
      val sprintsDataProvider = new JiraSprintsDataProvider(jiraConfig)
      val tasksDataProvider = new JiraTasksDataProvider(jiraConfig)
      IntegrationProviders(sprintsDataProvider, tasksDataProvider)
  }

  private val supportedProviders = Seq(tryJiraProvider)

}
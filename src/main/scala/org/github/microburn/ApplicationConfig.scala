package org.github.microburn

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.github.microburn.domain.ProjectConfig
import org.github.microburn.integration.jira.{JiraConfig, JiraSprintsDataProvider, JiraTasksDataProvider}
import org.github.microburn.service.IntegrationProviders

case class ApplicationConfig(jettyPort: Int,
                             updatePeriodSeconds: Int,
                             initialFetchToSprintStartAcceptableDelayMinutes: Int,
                             projectConfig: ProjectConfig,
                             integrationProviders: IntegrationProviders)

object ApplicationConfig {
  
  def apply(config: Config): ApplicationConfig = {
    val jettyPort = config.getInt("jetty.port")
    val updatePeriodSeconds = config.getDuration("integration.updatePeriodSeconds", TimeUnit.SECONDS).toInt
    val initialFetchToSprintStartAcceptableDelayMinutes =
      config.getDuration("history.initialFetchToSprintStartAcceptableDelayMinutes", TimeUnit.MINUTES).toInt

    val projectConfig = ProjectConfig(config)
    val providers = firstConfiguredProvider.applyOrElse(
      config,
      (_:Config) => throw new IllegalArgumentException("You must define configuration of service that you want to integrate with")
    )

    ApplicationConfig(
      jettyPort = jettyPort,
      updatePeriodSeconds = updatePeriodSeconds,
      initialFetchToSprintStartAcceptableDelayMinutes = initialFetchToSprintStartAcceptableDelayMinutes,
      projectConfig = projectConfig,
      integrationProviders = providers)
  }

  private val tryJiraProvider: PartialFunction[Config, IntegrationProviders] = {
    case config if config.hasPath("jira") =>
      val jiraConfig = JiraConfig(config.getConfig("jira"))
      val sprintsDataProvider = new JiraSprintsDataProvider(jiraConfig)
      val tasksDataProvider = new JiraTasksDataProvider(jiraConfig)
      IntegrationProviders(sprintsDataProvider, tasksDataProvider)
  }

  private val supportedProviders = Seq(tryJiraProvider)

  private val firstConfiguredProvider: PartialFunction[Config, IntegrationProviders] =
    supportedProviders.foldLeft(PartialFunction.empty[Config, IntegrationProviders]) { case (acc, tryProvider) =>
      acc orElse tryProvider
    }

}
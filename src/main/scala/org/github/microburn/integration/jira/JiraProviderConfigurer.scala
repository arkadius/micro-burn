package org.github.microburn.integration.jira

import com.typesafe.config.Config
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.support.scrum.ScrumIntegrationProvider
import org.github.microburn.integration.{IntegrationProvider, IntegrationProviderConfigurer}

object JiraProviderConfigurer extends IntegrationProviderConfigurer {
  override def tryConfigure: PartialFunction[Config, ProjectActor => IntegrationProvider] = {
    case config if config.hasPath("jira") =>
      val jiraConfig = JiraConfig(config.getConfig("jira"))
      val sprintsDataProvider = new JiraSprintsDataProvider(jiraConfig)
      val tasksDataProvider = new JiraTasksDataProvider(jiraConfig)
      new ScrumIntegrationProvider(sprintsDataProvider, tasksDataProvider)(_)
  }
}

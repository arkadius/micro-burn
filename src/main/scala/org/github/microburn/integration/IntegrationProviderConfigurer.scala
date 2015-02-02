package org.github.microburn.integration

import com.typesafe.config.Config
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.jira.JiraProviderConfigurer

trait IntegrationProviderConfigurer {
  def tryConfigure: PartialFunction[Config, ProjectActor => IntegrationProvider]
}

object IntegrationProviderConfigurer {
  private val supportedProviders = Seq(JiraProviderConfigurer)

  val firstConfiguredProvidersFactory: PartialFunction[Config, ProjectActor => IntegrationProvider] =
    supportedProviders.map(_.tryConfigure).foldLeft(PartialFunction.empty[Config, ProjectActor => IntegrationProvider]) { case (acc, tryConfigure) =>
      acc orElse tryConfigure
    }
}
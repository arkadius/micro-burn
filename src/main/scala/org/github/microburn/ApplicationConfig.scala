/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.github.microburn.domain.ProjectConfig
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.{IntegrationProvider, IntegrationProviderConfigurer}

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(connectorConfig: ConnectorConfig,
                             projectConfig: ProjectConfig,
                             durations: DurationsConfig,
                             integrationProvidersFactory: ProjectActor => IntegrationProvider)

object ApplicationConfig {
  
  def apply(config: Config): ApplicationConfig = {
    val durations = DurationsConfig(config.getConfig("durations"))
    val partiallyPreparedConfig = PartiallyPreparedConfig(durations)
    val providersFactory = IntegrationProviderConfigurer.firstConfiguredProvidersFactory(partiallyPreparedConfig).applyOrElse(
      config,
      (_:Config) => throw new IllegalArgumentException("You must define configuration of service that you want to integrate with")
    )

    ApplicationConfig(
      connectorConfig = ConnectorConfig(config.getConfig("connector")),
      projectConfig = ProjectConfig(config.getConfig("project")),
      durations = durations,
      integrationProvidersFactory = providersFactory)
  }

}

case class ConnectorConfig(port: Int, contextPath: String)

object ConnectorConfig {
  def apply(config: Config): ConnectorConfig = {
    ConnectorConfig(
      config.getInt("port"),
      config.getString("contextPath")
    )
  }
}

case class PartiallyPreparedConfig(durations: DurationsConfig)

case class DurationsConfig(initializationTimeout: FiniteDuration,
                           fetchPeriod: FiniteDuration,
                           initialFetchToSprintStartAcceptableDelayMinutes: FiniteDuration)

object DurationsConfig {
  import concurrent.duration._

  def apply(config: Config): DurationsConfig = {
    DurationsConfig(
      config.getDuration("initializationTimeout", TimeUnit.MILLISECONDS).millis,
      config.getDuration("fetchPeriod", TimeUnit.MILLISECONDS).millis,
      config.getDuration("initialFetchToSprintStartAcceptableDelay", TimeUnit.MILLISECONDS).millis
    )
  }
}
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
import org.github.microburn.integration.{Integration, IntegrationConfigurer}
import org.joda.time.Days

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(connectorConfig: ConnectorConfig,
                             projectConfig: ProjectConfig,
                             durations: DurationsConfig,
                             authorizationConfig: AuthorizationConfig,
                             integrationFactory: ProjectActor => Integration)

object ApplicationConfig {
  
  def apply(config: Config): ApplicationConfig = {
    val durations = DurationsConfig(config.getConfig("durations"))
    val partiallyPreparedConfig = PartiallyPreparedConfig(durations)
    val integrationFactory = IntegrationConfigurer.firstConfiguredIntegration(partiallyPreparedConfig).applyOrElse(
      config,
      (_:Config) => throw new IllegalArgumentException("You must define configuration of service that you want to integrate with")
    )

    ApplicationConfig(
      connectorConfig = ConnectorConfig(config.getConfig("connector")),
      projectConfig = ProjectConfig(config.getConfig("project")),
      durations = durations,
      authorizationConfig = AuthorizationConfig(config.getConfig("authorization")),
      integrationFactory = integrationFactory)
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

case class AuthorizationConfig(secretForScrumSimulation: Option[String])

object AuthorizationConfig {
  import org.github.microburn.util.config.ConfigExtensions._

  def apply(config: Config): AuthorizationConfig = {
    AuthorizationConfig(
      config.optional(_.getString, "secretForScrumSimulation")
    )
  }
}

case class PartiallyPreparedConfig(durations: DurationsConfig)

case class DurationsConfig(initializationTimeout: FiniteDuration,
                           fetchPeriod: FiniteDuration,
                           initialFetchToSprintStartAcceptableDelayMinutes: FiniteDuration,
                           clientFetchIfNoChangesPeriod: FiniteDuration,
                           defaultSprintDuration: Days)

object DurationsConfig {
  import scala.concurrent.duration._

  def apply(config: Config): DurationsConfig = {
    DurationsConfig(
      config.getDuration("initializationTimeout", TimeUnit.MILLISECONDS).millis,
      config.getDuration("fetchPeriod", TimeUnit.MILLISECONDS).millis,
      config.getDuration("initialFetchToSprintStartAcceptableDelay", TimeUnit.MILLISECONDS).millis,
      config.getDuration("clientFetchIfNoChangesPeriod", TimeUnit.MILLISECONDS).millis,
      Days.days(config.getDuration("defaultSprintDuration", TimeUnit.DAYS).toInt)
    )
  }
}
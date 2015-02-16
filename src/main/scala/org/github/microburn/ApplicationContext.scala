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

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.Integration
import org.github.microburn.service.{ProjectUpdater, SprintColumnsHistoryProvider}
import org.joda.time.Days

class ApplicationContext private(val projectActor: ProjectActor,
                                 val updater: ProjectUpdater,
                                 val integration: Integration,
                                 val columnsHistoryProvider: SprintColumnsHistoryProvider,
                                 appConfig: ApplicationConfig) {
  def connectorConfig: ConnectorConfig = appConfig.connectorConfig

  def authorizationConfig: AuthorizationConfig = appConfig.authorizationConfig

  def defaultSprintDuration: Days = appConfig.durations.defaultSprintDuration
}

object ApplicationContext {
  def apply(): ApplicationContext = context

  private lazy val context = {
    val configFile = System.getProperty("config", "application.conf")
    val config =
      ConfigFactory.parseFile(new File(configFile))
        .withFallback(ConfigFactory.parseResources("defaults.conf"))
        .resolveWith(ConfigFactory.parseResources("predefined.conf"))
    val appConfig = ApplicationConfig(config)
    val projectActor = new ProjectActor(appConfig.projectConfig, appConfig.durations.initialFetchToSprintStartAcceptableDelayMinutes)
    val integration = appConfig.integrationFactory(projectActor)
    val updater = new ProjectUpdater(integration, appConfig.durations.fetchPeriod)
    val columnsHistoryProvider = new SprintColumnsHistoryProvider(
      projectActor,
      appConfig.durations.initialFetchToSprintStartAcceptableDelayMinutes
    )(appConfig.projectConfig)

    new ApplicationContext(
      projectActor            = projectActor,
      updater                 = updater,
      integration             = integration,
      columnsHistoryProvider  = columnsHistoryProvider,
      appConfig               = appConfig)
  }
}
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
import org.github.microburn.integration.IntegrationProvider
import org.github.microburn.service.{ProjectUpdater, SprintColumnsHistoryProvider}

class ApplicationContext private(val projectActor: ProjectActor,
                                 val updater: ProjectUpdater,
                                 val integrationProvider: IntegrationProvider,
                                 val columnsHistoryProvider: SprintColumnsHistoryProvider,
                                 appConfig: ApplicationConfig) {
  def connectorConfig: ConnectorConfig = appConfig.connectorConfig
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
    val projectActor = new ProjectActor(appConfig.projectConfig)
    val integrationProvider = appConfig.integrationProvidersFactory(projectActor)
    val updater = new ProjectUpdater(integrationProvider, appConfig.durations.fetchPeriod)
    val columnsHistoryProvider = new SprintColumnsHistoryProvider(
      projectActor,
      appConfig.durations.initialFetchToSprintStartAcceptableDelayMinutes
    )(appConfig.projectConfig)

    new ApplicationContext(
      projectActor            = projectActor,
      updater                 = updater,
      integrationProvider     = integrationProvider,
      columnsHistoryProvider  = columnsHistoryProvider,
      appConfig               = appConfig)
  }
}
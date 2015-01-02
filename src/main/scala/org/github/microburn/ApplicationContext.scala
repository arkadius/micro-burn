package org.github.microburn

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.LiftActor
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.service.{ProjectUpdater, SprintColumnsHistoryProvider}

class ApplicationContext private(val projectActor: LiftActor,
                                 val updater: ProjectUpdater,
                                 val columnsHistoryProvider: SprintColumnsHistoryProvider,
                                 appConfig: ApplicationConfig) {
  def jettyPort: Int = appConfig.jettyPort
}

object ApplicationContext {
  def apply(): ApplicationContext = context

  private lazy val context = {
    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.parseResources("defaults.conf"))
    val appConfig = ApplicationConfig(config)
    val projectActor = new ProjectActor(appConfig.projectConfig)
    val updater = new ProjectUpdater(projectActor, appConfig.integrationProviders, appConfig.updatePeriodSeconds)
    val columnsHistoryProvider = new SprintColumnsHistoryProvider(
      projectActor,
      appConfig.initialFetchToSprintStartAcceptableDelayMinutes
    )(appConfig.projectConfig)

    new ApplicationContext(
      projectActor            = projectActor,
      updater                 = updater,
      columnsHistoryProvider  = columnsHistoryProvider,
      appConfig               = appConfig)
  }
}
package org.github.jiraburn

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.MockLiftActor
import org.github.jiraburn.domain.ProjectConfig
import org.github.jiraburn.domain.actors.ProjectActor
import org.github.jiraburn.jira.{JiraConfig, SprintsDataProvider, TasksDataProvider}
import org.github.jiraburn.service.{ProjectUpdater, SprintColumnsHistoryProvider}

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

    val jiraConfig = JiraConfig(config)

    val jettyPort = config.getInt("jetty.port")
    val jiraFetchPeriodSeconds = config.getInt("jira.fetchPeriodSeconds")
    val initialFetchToSpringStartAcceptableDelayMinutes = config.getInt("jira.initialFetchToSpringStartAcceptableDelayMinutes")

    val updater = new ProjectUpdater(projectActor, new SprintsDataProvider(jiraConfig), new TasksDataProvider(jiraConfig), initialFetchToSpringStartAcceptableDelayMinutes)
    val columnsHistoryProvider = new SprintColumnsHistoryProvider(projectActor, initialFetchToSpringStartAcceptableDelayMinutes)
    new ApplicationContext(
      updater                 = updater,
      columnsHistoryProvider  = columnsHistoryProvider,
      jettyPort               = jettyPort)
  }
}

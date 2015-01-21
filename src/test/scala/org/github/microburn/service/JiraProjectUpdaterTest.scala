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
package org.github.microburn.service

import java.util.Locale

import org.github.microburn.ConfigUtils
import org.github.microburn.domain.ProjectConfig
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration._
import org.github.microburn.integration.jira._
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{Directives, Route}

import scala.concurrent.duration._
import scala.reflect.io.Path

class JiraProjectUpdaterTest extends FlatSpec with RestIntegrationTest with Directives with Matchers {
  import org.github.microburn.util.concurrent.FutureEnrichments._

  override def route: Route = JiraSprintsDataProviderTest.route ~ JiraTasksDataProviderTest.route

  it should "fetch inital project state" in {
//    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigUtils.withToDefaultsFallback)
    val config = ConfigUtils.withToDefaultsFallback
    val projectConfig = ProjectConfig(config)
    Path(projectConfig.dataRoot).deleteRecursively()
    val projectActor = new ProjectActor(projectConfig)
    val jiraConfig = JiraConfig(config.getConfig("jira"))
    val providers = IntegrationProviders(new JiraSprintsDataProvider(jiraConfig, Locale.ENGLISH), new JiraTasksDataProvider(jiraConfig))
    val updater = new ProjectUpdater(projectActor, providers, updatePeriodSeconds = 123)
    updater.updateProject().await(10 seconds)
  }

}
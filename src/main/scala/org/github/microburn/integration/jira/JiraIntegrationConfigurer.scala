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
package org.github.microburn.integration.jira

import com.typesafe.config.Config
import org.github.microburn.PartiallyPreparedConfig
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.support.scrum.ScrumIntegration
import org.github.microburn.integration.{Integration, IntegrationConfigurer}

object JiraIntegrationConfigurer extends IntegrationConfigurer {
  override def tryConfigure(partiallyPreparedConfig: PartiallyPreparedConfig): PartialFunction[Config, ProjectActor => Integration] = {
    case config if config.hasPath("jira") =>
      val jiraConfig = JiraConfig(config.getConfig("jira"))
      val sprintsDataProvider = new JiraSprintsDataProvider(jiraConfig)
      val tasksDataProvider = new JiraTasksDataProvider(jiraConfig)
      new ScrumIntegration(sprintsDataProvider, tasksDataProvider)(_)
  }
}
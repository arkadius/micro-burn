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
package org.github.microburn.integration

import com.typesafe.config.Config
import org.github.microburn.{PartiallyPreparedConfig, DurationsConfig}
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.jira.JiraIntegrationConfigurer
import org.github.microburn.integration.trello.TrelloConfigurer

trait IntegrationConfigurer {
  def tryConfigure(partiallyPreparedConfig: PartiallyPreparedConfig): PartialFunction[Config, ProjectActor => Integration]
}

object IntegrationConfigurer {
  private val supportedIntegrations = Seq(JiraIntegrationConfigurer, TrelloConfigurer)

  def firstConfiguredIntegration(partiallyPreparedConfig: PartiallyPreparedConfig): PartialFunction[Config, ProjectActor => Integration] =
    supportedIntegrations.map(_.tryConfigure(partiallyPreparedConfig)).foldLeft(PartialFunction.empty[Config, ProjectActor => Integration]) { case (acc, tryConfigure) =>
      acc orElse tryConfigure
    }
}
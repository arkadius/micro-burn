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
package org.github.microburn.domain

import org.github.microburn.TestConfig

object ProjectConfigUtils {
  def completedColumnIndex(implicit config: ProjectConfig): Int = config.boardColumnIndex(firstCompletedStatus)

  private def firstCompletedStatus(implicit config: ProjectConfig): String = config.boardColumns(config.lastDoneColumnIndex).statusIds.head

  def firstNotCompletedStatus(implicit config: ProjectConfig): String = config.nonBacklogColumns.head.statusIds.head

  def defaultConfig = ProjectConfig(TestConfig.withDefaultsFallback().getConfig("project"))

}
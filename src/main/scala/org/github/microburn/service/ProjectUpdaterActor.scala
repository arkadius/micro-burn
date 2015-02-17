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

import net.liftweb.actor.LAFuture
import org.github.microburn.integration.Integration
import org.github.microburn.util.concurrent.JobRepeatingActor
import org.github.microburn.util.logging.Slf4jLogging
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

class ProjectUpdaterActor(provider: Integration, fetchPeriod: FiniteDuration) extends JobRepeatingActor with Slf4jLogging {
  override protected val jobDescription: String = "updating project data"

  override protected val tickPeriod: FiniteDuration = fetchPeriod

  override protected def prepareFutureOfJob(timestamp: DateTime): LAFuture[_] =
    measureFuture("update of project")(provider.updateProject(timestamp.toDate))
}
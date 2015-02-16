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

import java.util.Date

import net.liftweb.common.Failure
import net.liftweb.util.Helpers._
import net.liftweb.util.Schedule
import org.github.microburn.integration.Integration
import org.github.microburn.util.logging.Slf4jLogging

import scala.concurrent.duration.FiniteDuration

class ProjectUpdater(provider: Integration, fetchPeriod: FiniteDuration) extends Slf4jLogging {

  import org.github.microburn.util.concurrent.FutureEnrichments._
  
  def start(): Unit = repeat()

  private def repeat(): Unit = {
    val timestamp = new Date
    measureFuture("update of project")(provider.updateProject(timestamp)).onComplete { result =>
      result match {
        case Failure(msg, ex, _) => error(s"Error while updating project data: ${ex.map(_.getMessage).openOr(msg)}")
        case _ =>
      }
      Schedule.schedule(() => repeat(), fetchPeriod.toTimeSpan)
    }
  }

}
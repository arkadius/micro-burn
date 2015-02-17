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
package org.github.microburn.integration.support.kanban

import java.io.File

import net.liftweb.actor.LAFuture
import org.github.microburn.integration.Integration
import org.github.microburn.repository.LastSprintRestartRepository
import org.github.microburn.util.concurrent.JobRepeatingActor
import org.github.microburn.util.logging.Slf4jLogging
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

class AutomaticScrumManagerActor(scrumSimulator: ScrumSimulatorActor,
                                 restartPeriod: RepeatPeriod,
                                 repo: LastSprintRestartRepository,
                                 protected val tickPeriod: FiniteDuration) extends JobRepeatingActor with Slf4jLogging  {
  import org.github.microburn.util.concurrent.ActorEnrichments._

  override protected val jobDescription: String = "scheduling next sprint restart"

  private val nextRestartComputer = new NextRestartComputer(restartPeriod)

  private var scheduledRestart: NextRestart = nextRestartComputer.compute(repo.loadLastSprintRestart)

  override protected def prepareFutureOfJob(timestamp: DateTime): LAFuture[_] = {
    if (timestamp.isBefore(scheduledRestart.date)) {
      LAFuture(() => Unit)
    } else {
      repo.saveLastSprintRestart(timestamp)
      val nextRestart = nextRestartComputer.compute(Some(timestamp))
      val start = StartSprint(s"Sprint ${scheduledRestart.periodName}", scheduledRestart.date.toDate, nextRestart.date.toDate)
      scheduledRestart = nextRestart
      for {
        _ <- scrumSimulator ?? FinishCurrentActiveSprint
        startResult <- scrumSimulator ?? start 
      } yield startResult
    }
  }
}

object AutomaticScrumManagerActor {
  def optionallyPrepareAutomaticScrumManager(optionalScrumManagementMode: Option[ScrumManagementMode],
                                             integration: Integration,
                                             projectRoot: File,
                                             tickPeriod: FiniteDuration): Option[AutomaticScrumManagerActor] = {
    integration match {
      case scrumSimulation: ScrumSimulation =>
        val managementMode = optionalScrumManagementMode.getOrElse {
          throw new IllegalStateException("You must define management mode")
        }
        managementMode match {
          case auto: AutomaticManagementMode =>
            Some(prepare(projectRoot, tickPeriod, scrumSimulation.scrumSimulator, auto.restartPeriod))
          case otherMode =>
            None
        }
      case notSimulating =>
        None
    }
  }

  private def prepare(projectRoot: File,
                      tickPeriod: FiniteDuration,
                      scrumSimulator: ScrumSimulatorActor,
                      restartPeriod: RepeatPeriod): AutomaticScrumManagerActor = {
    val repo = LastSprintRestartRepository(projectRoot)
    new AutomaticScrumManagerActor(scrumSimulator, restartPeriod, repo, tickPeriod)
  }
}
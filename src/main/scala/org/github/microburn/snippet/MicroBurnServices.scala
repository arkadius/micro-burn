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
package org.github.microburn.snippet

import net.liftmodules.ng.Angular._
import net.liftweb.http.S
import net.liftweb.http.js.JsCmds.{JsCrVar, Script}
import net.liftweb.http.js.JsExp
import org.github.microburn.ApplicationContext
import org.github.microburn.integration.support.kanban._
import org.github.microburn.service.GetColumnsHistory
import org.github.microburn.util.logging.Slf4jLogging
import scalaz.Scalaz._

object MicroBurnServices extends Slf4jLogging {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.ActorEnrichments._
  
  def render = {
    val configuredSecret = ApplicationContext().authorizationConfig.secretForScrumSimulation
    val givenSecret = S.param("secret").toOption
    val optionalScrumManagement = ApplicationContext().integration match {
      case s: ScrumSimulation if configuredSecret.isEmpty | givenSecret == configuredSecret =>
        Some(ScrumManagement(s.scrumSimulator,  !ApplicationContext().hasAutomaticScrumManagement))
      case _ =>
        None
    }
    Script(
      JsCrVar("sprintsManagement", JsExp.boolToJsExp(optionalScrumManagement.exists(_.sprintsManagementEnabled))) &
      JsCrVar("baseManagement", JsExp.boolToJsExp(optionalScrumManagement.isDefined)) &
      JsCrVar("clientFetchIfNoChangesPeriod", JsExp.longToJsExp(ApplicationContext().clientFetchIfNoChangesPeriod.toMillis)) &
      JsCrVar("defaultSprintDuration", JsExp.intToJsExp(ApplicationContext().defaultSprintDuration.getDays))
    ) +: renderIfNotAlreadyDefined {
      val module = angular.module("MicroBurnServices")
        .factory("historySvc", jsObjFactory()
          .future("getHistory", (getHistory: GetColumnsHistory) =>
            measureFuture("history computation"){
              ApplicationContext().columnsHistoryProvider.columnsHistory(getHistory.sprintId)
            }
          )
        )
      optionalScrumManagement.map {
        case ScrumManagement(scrumSimulator, sprintsManagementEnabled) =>
          val baseManagement = jsObjFactory()
            .future("defineBase", (base: DefineBaseStoryPoints) => (scrumSimulator ?? base).mapToBox)
          val operations = sprintsManagementEnabled.option(
            baseManagement
              .future[StartSprint, Any]("startSprint", (start: StartSprint) => (scrumSimulator ?? start).mapToBox)
              .future("finishSprint", (finish: FinishSprint) => (scrumSimulator ?? finish).mapToBox)
              .future("removeSprint", (remove: RemoveSprint) => (scrumSimulator ?? remove).mapToBox)
              .future("updateStartDate", (start: UpdateStartDate) => (scrumSimulator ?? start).mapToBox)
              .future("updateEndDate", (end: UpdateEndDate) => (scrumSimulator ?? end).mapToBox)
          ).getOrElse(baseManagement)
          module.factory("scrumSimulatorSvc", operations)
      } getOrElse module
    }
  }

  case class ScrumManagement(simulator: ScrumSimulatorActor, sprintsManagementEnabled: Boolean)
}
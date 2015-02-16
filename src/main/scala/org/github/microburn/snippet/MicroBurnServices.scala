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
import org.github.microburn.util.logging.Slf4jLogging

object MicroBurnServices extends Slf4jLogging {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._
  
  def render = {
    val givenSecret = S.param("secret").toOption
    val scrumSimulation = ApplicationContext().integration match {
      case s: ScrumSimulation if givenSecret == ApplicationContext().authorizationConfig.secretForScrumSimulation => Some(s.scrumSimulator)
      case s: ScrumSimulation if ApplicationContext().authorizationConfig.secretForScrumSimulation.isEmpty => Some(s.scrumSimulator)
      case _ => None
    }
    Script(
      JsCrVar("scrumSimulation", JsExp.boolToJsExp(scrumSimulation.isDefined)) &
      JsCrVar("defaultSprintDuration", JsExp.intToJsExp(ApplicationContext().defaultSprintDuration.getDays))
    ) +: renderIfNotAlreadyDefined {
      val module = angular.module("MicroBurnServices")
        .factory("historySvc", jsObjFactory()
        .future("getHistory", (sprintId: String) => measureFuture("renderable history computation")(ApplicationContext().columnsHistoryProvider.columnsHistory(sprintId)))
        )
      scrumSimulation.map { scrumSimulator =>
        module.factory("scrumSimulatorSvc", jsObjFactory()
          .future[StartSprint, Any]("startSprint", (start: StartSprint) => (scrumSimulator ?? start).mapToBox)
          .future("finishSprint", (sprintId: String) => (scrumSimulator ?? FinishSprint(sprintId)).mapToBox)
          .future("removeSprint", (sprintId: String) => (scrumSimulator ?? RemoveSprint(sprintId)).mapToBox)
          .future("updateStartDate", (start: UpdateStartDate) => (scrumSimulator ?? start).mapToBox)
          .future("updateEndDate", (end: UpdateEndDate) => (scrumSimulator ?? end).mapToBox)
          .future("defineBase", (base: DefineBaseStoryPoints) => (scrumSimulator ?? base).mapToBox)
        )
      } getOrElse module
    }
  }
}
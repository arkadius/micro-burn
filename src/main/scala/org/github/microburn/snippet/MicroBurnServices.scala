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
import net.liftweb.common.Full
import org.github.microburn.ApplicationContext
import org.github.microburn.domain.actors.FinishSprint
import org.github.microburn.integration.support.kanban.{DoFinishSprint, FinishCurrentSprint, StartSprint, ScrumSimulation}
import org.github.microburn.service.ColumnsHistory

object MicroBurnServices {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.LiftActorEnrichments._

  def render = renderIfNotAlreadyDefined {
    val module = angular.module("MicroBurnServices")
      .factory("historySvc", jsObjFactory()
        .future("getHistory", (sprintId: String) => ApplicationContext().columnsHistoryProvider.columnsHistory(sprintId))
      )
    ApplicationContext().integrationProvider match {
      case s: ScrumSimulation =>
        module.factory("scrumSimulatorSvc", jsObjFactory()
          .future[StartSprint, Any]("startSprint",  (start: StartSprint) => (s.scrumSimulator ?? start).boxed)
          .future("finishSprint", (sprintId: String) => (s.scrumSimulator ?? DoFinishSprint(sprintId)).boxed)
        )
      case _ =>
        module
    }
  }
}
package org.github.microburn.snippet

import net.liftmodules.ng.Angular._
import org.github.microburn.ApplicationContext

object MicroBurnServices {
  def render = renderIfNotAlreadyDefined(
    angular.module("MicroBurnServices")
      .factory("historySvc", jsObjFactory()
        .future("getHistory", (sprintId: String) =>
          ApplicationContext().columnsHistoryProvider.columnsHistory(sprintId)
        )
      )
  )
}

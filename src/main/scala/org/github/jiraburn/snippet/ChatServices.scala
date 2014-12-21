package org.github.jiraburn.snippet

import net.liftmodules.ng.Angular._
import net.liftweb.common.Empty
import org.github.jiraburn.comet.ChatServer

object ChatServices {
  def render = renderIfNotAlreadyDefined(
    angular.module("ChatServices")
      .factory("chatSvc", jsObjFactory()
        .jsonCall("sendChat", (chat:String) => {
          println(chat)
          ChatServer ! chat
          Empty
        })
      )
  )
}

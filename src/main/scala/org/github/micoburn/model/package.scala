package org.github.micoburn

import net.liftmodules.ng.Angular.NgModel

package object model {
  case class User(ip:String)
  case class ChatMessages(msgs:List[String]=List()) extends NgModel
}

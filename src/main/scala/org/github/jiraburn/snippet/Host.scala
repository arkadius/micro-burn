package org.github.jiraburn.snippet

import net.liftweb.util.Helpers._
import java.net.InetAddress

class Host {
  def render = "*" #> InetAddress.getLocalHost.getHostAddress
}

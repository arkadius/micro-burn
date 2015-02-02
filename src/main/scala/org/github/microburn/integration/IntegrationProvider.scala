package org.github.microburn.integration

import net.liftweb.actor.LAFuture

trait IntegrationProvider {
  def updateProject(): LAFuture[_]
}
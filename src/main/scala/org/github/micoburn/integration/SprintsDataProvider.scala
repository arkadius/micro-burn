package org.github.micoburn.integration

import net.liftweb.actor.LAFuture
import org.github.micoburn.domain.SprintDetails

trait SprintsDataProvider {
  def allSprintIds: LAFuture[Seq[Long]]
  def sprintDetails(sprintId: String): LAFuture[SprintDetails]
}

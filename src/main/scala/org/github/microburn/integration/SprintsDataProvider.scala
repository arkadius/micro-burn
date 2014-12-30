package org.github.microburn.integration

import net.liftweb.actor.LAFuture
import org.github.microburn.domain.SprintDetails

trait SprintsDataProvider {
  def allSprintIds: LAFuture[Seq[Long]]
  def sprintDetails(sprintId: String): LAFuture[SprintDetails]
}

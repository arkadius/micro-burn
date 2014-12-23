package org.github.jiraburn.integration

import net.liftweb.actor.LAFuture
import org.github.jiraburn.domain.SprintDetails

trait SprintsDataProvider {
  def allSprintIds: LAFuture[Seq[Long]]
  def sprintDetails(sprintId: String): LAFuture[SprintDetails]
}

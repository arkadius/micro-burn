package org.github.jiraburn.integration

import net.liftweb.actor.LAFuture
import org.github.jiraburn.domain.UserStory

trait TasksDataProvider {

  def userStories(sprintId: String): LAFuture[Seq[UserStory]]

}

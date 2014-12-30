package org.github.microburn.integration

import net.liftweb.actor.LAFuture
import org.github.microburn.domain.UserStory

trait TasksDataProvider {

  def userStories(sprintId: String): LAFuture[Seq[UserStory]]

}

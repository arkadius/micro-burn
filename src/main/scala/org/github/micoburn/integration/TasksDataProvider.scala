package org.github.micoburn.integration

import net.liftweb.actor.LAFuture
import org.github.micoburn.domain.UserStory

trait TasksDataProvider {

  def userStories(sprintId: String): LAFuture[Seq[UserStory]]

}

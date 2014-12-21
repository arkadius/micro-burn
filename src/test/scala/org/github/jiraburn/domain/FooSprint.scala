package org.github.jiraburn.domain

import java.util.Date

object FooSprint {

  def withEmptyEvents(userStories: UserStory*): Sprint = {
    val details = SprintDetails(
      name = "fooName",
      start = new Date(1000),
      end = new Date(2000)
    )
    Sprint.withEmptyEvents("foo", details, SprintState(userStories, new Date(2000)))
  }
  
}

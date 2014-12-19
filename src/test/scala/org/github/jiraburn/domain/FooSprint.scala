package org.github.jiraburn.domain

import java.util.Date

object FooSprint {

  def withEmptyEvents(userStories: UserStory*): Sprint = {
    val details = SprintDetails(
      name = "fooName",
      from = new Date(1000),
      to = new Date(2000)
    )
    Sprint.withEmptyEvents("foo", details, userStories)
  }
  
}

package com.example.domain

import java.util.Date

object FooSprint {

  def withEmptyEvents(userStories: Seq[UserStory]): Sprint = {
    val details = SprintDetails(
      name = "fooName",
      from = new Date(1000),
      to = new Date(2000),
      isActive = true
    )
    Sprint.withEmptyEvents("foo", details, userStories)
  }
  
}

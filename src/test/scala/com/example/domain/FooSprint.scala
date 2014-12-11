package com.example.domain

object FooSprint {

  def withEmptyEvents(userStories: Seq[UserStory]): Sprint =
    Sprint.withEmptyEvents("foo", userStories)
  
}

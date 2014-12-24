package org.github.jiraburn.domain

import java.util.Date

object SampleSprint {

  private val sampleDetails = SprintDetails(
    name = "fooName",
    start = new Date(1000),
    end = new Date(2000)
  )

  def withEmptyEvents(userStories: UserStory*): Sprint = {
    Sprint.withEmptyEvents("foo", sampleDetails, BoardState(userStories, new Date(2000)))
  }
  
}

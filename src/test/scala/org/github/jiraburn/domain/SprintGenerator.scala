package org.github.jiraburn.domain

import org.scalacheck.Gen

object SprintGenerator {

  def withEmptyEvents: Gen[Sprint] =
    for {
      userStories <- Gen.listOf(TasksGenerator.userStoryGenerator)
    } yield SampleSprint.withEmptyEvents(userStories : _*)
  
}

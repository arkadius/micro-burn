package org.github.jiraburn.domain.generator

import org.github.jiraburn.domain.{SampleSprint, Sprint}
import org.scalacheck.Gen

object SprintGenerator {

  def withEmptyEvents: Gen[Sprint] = {
    val id = "foo"
    val details = SampleSprint.sampleDetails
    for {
      boardState <- BoardStateGenerator.generator(details.end)
    } yield Sprint.withEmptyEvents(id, details, boardState)
  }

}

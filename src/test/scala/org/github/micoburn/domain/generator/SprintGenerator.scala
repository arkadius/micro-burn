package org.github.micoburn.domain.generator

import org.github.micoburn.domain.{SampleSprint, Sprint}
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

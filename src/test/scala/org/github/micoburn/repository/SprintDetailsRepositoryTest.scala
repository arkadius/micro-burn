package org.github.micoburn.repository

import java.io.File

import org.github.micoburn.domain.SampleSprint
import org.scalatest.{Matchers, FlatSpec}

class SprintDetailsRepositoryTest extends FlatSpec with Matchers {

  import org.scalatest.OptionValues._

  it should "work round trip" in {
    val sprint = SampleSprint.withEmptyEvents()
    val repo = SprintDetailsRepository(new File(s"target/sprints/${sprint.id}"))

    repo.saveDetails(sprint)

    repo.loadDetails.value shouldEqual sprint.details
  }

}

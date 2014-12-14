package com.example.repository

import java.io.File

import com.example.domain.FooSprint
import org.scalatest.{Matchers, FlatSpec}

class SprintDetailsRepositoryTest extends FlatSpec with Matchers {

  import org.scalatest.OptionValues._

  it should "work round trip" in {
    val sprint = FooSprint.withEmptyEvents(Nil)
    val repo = SprintDetailsRepository(new File(s"target/sprints/${sprint.id}"))

    repo.saveDetails(sprint)

    repo.loadDetails.value shouldEqual sprint.details
  }

}

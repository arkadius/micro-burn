package org.github.jiraburn.domain

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec, FunSuite}

class ProjectConfigTest extends FlatSpec with Matchers {

  it should "create project config from conf" in {
    val config = ProjectConfig(ConfigFactory.load())

    config.boardColumns should have length 5
  }

}

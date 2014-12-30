package org.github.microburn.domain

import com.typesafe.config.ConfigFactory
import org.github.microburn.ConfigUtils
import org.scalatest.{Matchers, FlatSpec, FunSuite}

class ProjectConfigTest extends FlatSpec with Matchers {

  it should "create project config from conf" in {
    val config = ProjectConfig(ConfigUtils.withToDefaultsFallback)

    config.boardColumns should have length 5
  }

}

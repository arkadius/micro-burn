package org.github.micoburn.domain

import com.typesafe.config.ConfigFactory
import org.github.micoburn.ConfigUtils
import org.scalatest.{Matchers, FlatSpec, FunSuite}

class ProjectConfigTest extends FlatSpec with Matchers {

  it should "create project config from conf" in {
    val config = ProjectConfig(ConfigUtils.withToDefaultsFallback)

    config.boardColumns should have length 5
  }

}

package org.github.micoburn

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtils {

  def withToDefaultsFallback: Config = ConfigFactory.load().withFallback(ConfigFactory.parseResources("defaults.conf"))

}

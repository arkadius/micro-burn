package org.github.microburn.domain

import java.text.ParseException

import com.typesafe.config.Config

sealed trait SprintBaseDetermineMode

case object AutomaticOnSprintStartMode extends SprintBaseDetermineMode

case class SpecifiedBaseMode(storyPoints: BigDecimal) extends SprintBaseDetermineMode

case object AutomaticOnScopeChangeMode extends SprintBaseDetermineMode

object SprintBaseDetermineModeParser {
  import org.github.microburn.util.config.ConfigEnrichments._

  def parse(config: Config): Option[SprintBaseDetermineMode] = {
    val Specified = "specified\\((.*)\\)".r("number")
    config.optional(_.getString, "sprint-base").map {
      case "auto-on-sprint-start" => AutomaticOnSprintStartMode 
      case "auto-on-scope-change" => AutomaticOnScopeChangeMode
      case Specified(number) => SpecifiedBaseMode(BigDecimal(number))
      case otherMode => throw new ParseException(s"Illegal sprint base determine mode: $otherMode", -1)
    }
  }
}

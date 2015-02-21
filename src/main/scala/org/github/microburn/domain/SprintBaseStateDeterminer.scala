/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.domain

import java.text.ParseException

import com.typesafe.config.Config

class SprintBaseStateDeterminer(baseDetermineMode: SprintBaseDetermineMode) {

  def baseForSprint(details: SprintDetails,
                    storyPointsSumOnStart: => BigDecimal,
                    lastStoryPointsSum: => BigDecimal): BigDecimal = {
    details.overriddenBaseStoryPointsSum getOrElse (baseDetermineMode match {
      case SpecifiedBaseMode(storyPoints) =>
        storyPoints
      case AutomaticOnSprintStartMode =>
        storyPointsSumOnStart
      case AutomaticOnScopeChangeMode =>
        lastStoryPointsSum
    })
  }
}

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
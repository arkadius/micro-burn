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
package org.github.microburn.integration.support.kanban

import com.typesafe.config.ConfigFactory
import org.github.microburn.util.date.Time
import org.joda.time.{DateTime, DateTimeConstants}
import org.scalatest.{Matchers, FlatSpec}

class ScrumManagementModeParserTest extends FlatSpec with Matchers {

  it should "handle none management mode" in {
    ScrumManagementModeParser.parse(ConfigFactory.parseString("")) shouldBe None
  }

  it should "parse manual mode" in {
    ScrumManagementModeParser.parse(ConfigFactory.parseString(
      """management {
        |  mode = manual
        |}
      """.stripMargin)) shouldBe Some(ManualManagementMode(automaticScopeChange = false))
  }

  it should "parse manual mode with automaticScopeChange specified" in {
    ScrumManagementModeParser.parse(ConfigFactory.parseString(
      """management {
        |  mode = manual
        |  automaticScopeChange = true
        |}
      """.stripMargin)) shouldBe Some(ManualManagementMode(automaticScopeChange = true))
  }

  it should "parse automatic mode with every-n-days period" in {
    ScrumManagementModeParser.parse(ConfigFactory.parseString(
      """management {
        |  mode = auto
        |  period = every-n-days
        |}
      """.stripMargin)) shouldBe Some(AutomaticManagementMode(EveryNDays(1, Time(0, 0), None)))
  }

  it should "parse automatic mode with every-n-weeks period" in {
    ScrumManagementModeParser.parse(ConfigFactory.parseString(
      """management {
        |  mode = auto
        |  period = every-n-weeks
        |  n = 2
        |  day-of-week = tuesday
        |  time = "02:11"
        |}
      """.stripMargin)) shouldBe Some(AutomaticManagementMode(EveryNWeeks(2, DateTimeConstants.TUESDAY, Time(2, 11), None)))
  }

  it should "parse automatic mode with every-n-months period" in {
    ScrumManagementModeParser.parse(ConfigFactory.parseString(
      """management {
        |  mode = auto
        |  period = every-n-months
        |  day-of-month = 13
        |  start-date = "2015-06-01"
        |}
      """.stripMargin)) shouldBe Some(AutomaticManagementMode(EveryNMonths(1, 13, Time(0, 0), Some(new DateTime(2015, 6, 1, 0, 0)))))
  }

}
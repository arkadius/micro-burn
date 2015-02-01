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
package org.github.microburn.integration.trello

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, FlatSpec}

class StoryPointsFromNameTest extends FlatSpec with TableDrivenPropertyChecks with Matchers {

  val checks = Table(
    ("nameWithOptionalSp", "optionalStoryPoints", "name"),
    ("without sp",    None,      "without sp"),
    ("(123) with sp", Some(BigDecimal(123)), "with sp"),
    ("(0.5) with sp", Some(BigDecimal("0.5")), "with sp"),
    ("(0.25) with sp", Some(BigDecimal("0.25")), "with sp"),
    ("(0.125) with sp", Some(BigDecimal("0.125")), "with sp"),
    ("(0.9999) with too much precise sp", None, "(0.9999) with too much precise sp")
  )

  it should "extract story points from name well" in {
    forAll(checks) { (nameWithOptionalSp,  expectedOptionalStoryPoints, expectedName) =>
      val StoryPointsFromName(resultOptionalStoryPoints, resultName) = nameWithOptionalSp
      resultOptionalStoryPoints shouldEqual expectedOptionalStoryPoints
      resultName shouldEqual expectedName
    }
  }

}
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

import org.github.microburn.domain.EveryNDays
import org.github.microburn.util.date.Time
import org.joda.time.{DateTimeConstants, DateTime}
import org.scalatest.{Matchers, FlatSpec}

class NextRestartComputerTest extends FlatSpec with Matchers {

  it should "compute restart for every-n-days if no last repeat date specified and current before time" in {
    val nDays = EveryNDays(n = 3, Time(8, 10), None)
    val computer = new NextRestartComputer(nDays)

    val result = computer.compute(None, new DateTime(2015, 1, 1, 0, 0))

    result shouldEqual NextRestart(new DateTime(2015, 1, 1, 8, 10), "2015.001")
  }

  it should "compute restart for every-n-days if no last repeat date specified and current after time" in {
    val nDays = EveryNDays(n = 3, Time(8, 10), None)
    val computer = new NextRestartComputer(nDays)

    val result = computer.compute(None, new DateTime(2015, 1, 1, 9, 0))

    result shouldEqual NextRestart(new DateTime(2015, 1, 2, 8, 10), "2015.002")
  }

  it should "compute restart for every-n-days if some last repeat date specified" in {
    val nDays = EveryNDays(n = 3, Time(8, 10), None)
    val computer = new NextRestartComputer(nDays)

    val result = computer.compute(Some(new DateTime(2015, 1, 1, 8, 10)), new DateTime(2015, 1, 1, 9, 0))

    result shouldEqual NextRestart(new DateTime(2015, 1, 4, 8, 10), "2015.004")
  }

}
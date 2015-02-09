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
package org.github.microburn.service

import org.github.microburn.domain.ProjectConfigUtils
import org.joda.time._
import org.scalatest.{FlatSpec, Matchers}

class EstimateComputerTest extends FlatSpec with Matchers {
  implicit val config = ProjectConfigUtils.defaultConfig

  it should "calculate estimates for positive whole story points" in {
    val start = new DateTime(2014, 12, 18, 0, 0)
    val end = new DateTime(2014, 12, 19, 0, 0)
    val result = EstimateComputer.estimatesBetween(start, end, 1)
    result shouldEqual List(
      Probe(start, 1),
      Probe(end, 0)
    )
  }

  it should "calculate estimates for zero story points" in {
    val start = new DateTime(2014, 12, 18, 0, 0)
    val middle = new DateTime(2014, 12, 18, 8, 0)
    val end = new DateTime(2014, 12, 19, 0, 0)
    val result = EstimateComputer.estimatesBetween(start, end, BigDecimal("1.5"))
    result shouldEqual List(
      Probe(start, BigDecimal("1.5")),
      Probe(middle, 1),
      Probe(end, 0)
    )
  }

  it should "calculate estimates for postivie non whole story points" in {
    val start = new DateTime(2014, 12, 18, 0, 0)
    val end = new DateTime(2014, 12, 19, 0, 0)
    val result = EstimateComputer.estimatesBetween(start, end, 0)
    result shouldEqual List(
      Probe(start, 0),
      Probe(end, 0)
    )
  }

}
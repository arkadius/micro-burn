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

import org.joda.time._
import org.scalatest.{Matchers, FlatSpec}

class EstimateComputerTest extends FlatSpec with Matchers {

  it should "calculate good intervals for start in weekend" in {
    val result = EstimateComputer.businessWeekIntervals(new DateTime(2014, 12, 20, 0, 0), new DateTime(2014, 12, 23, 0, 0))
    result shouldEqual List(
      new Interval(new DateTime(2014, 12, 22, 0, 0), new DateTime(2014, 12, 23, 0, 0))
    )
  }

  it should "calculate good intervals for end in weekend" in {
    val result = EstimateComputer.businessWeekIntervals(new DateTime(2014, 12, 19, 0, 0), new DateTime(2014, 12, 21, 0, 0))
    result shouldEqual List(
      new Interval(new DateTime(2014, 12, 19, 0, 0), new DateTime(2014, 12, 20, 0, 0))
    )
  }

  it should "calculate good intervals for both start and end in weekend" in {
    val result = EstimateComputer.businessWeekIntervals(new DateTime(2014, 12, 20, 0, 0), new DateTime(2014, 12, 21, 0, 0))
    result shouldEqual Nil
  }

  it should "calculate good one interval" in {
    val result = EstimateComputer.businessWeekIntervals(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 19, 0, 0))
    result shouldEqual List(
      new Interval(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 19, 0, 0))
    )
  }

  it should "calculate good two intervals" in {
    val result = EstimateComputer.businessWeekIntervals(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 23, 0, 0))
    result shouldEqual List(
      new Interval(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 20, 0, 0)),
      new Interval(new DateTime(2014, 12, 22, 0, 0), new DateTime(2014, 12, 23, 0, 0))
    )
  }

  it should "calculate good tree intervals" in {
    val result = EstimateComputer.businessWeekIntervals(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 31, 0, 0))
    result shouldEqual List(
      new Interval(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 20, 0, 0)),
      new Interval(new DateTime(2014, 12, 22, 0, 0), new DateTime(2014, 12, 27, 0, 0)),
      new Interval(new DateTime(2014, 12, 29, 0, 0), new DateTime(2014, 12, 31, 0, 0))
    )
  }

  it should "calculate good sums" in {
    val firstInterval = new Interval(new DateTime(2014, 12, 18, 0, 0), new DateTime(2014, 12, 20, 0, 0))
    val secInterval   = new Interval(new DateTime(2014, 12, 22, 0, 0), new DateTime(2014, 12, 27, 0, 0))
    val thrdInterval  = new Interval(new DateTime(2014, 12, 29, 0, 0), new DateTime(2014, 12, 31, 0, 0))
    val intervals = List(firstInterval, secInterval, thrdInterval)

    val sums = EstimateComputer.intervalAndSumMillisAfterThem(intervals).toIndexedSeq
    sums(0).sumBefore shouldEqual 0
    sums(1).sumBefore shouldEqual firstInterval.toDurationMillis
    sums(2).sumBefore shouldEqual firstInterval.toDurationMillis + secInterval.toDurationMillis
  }

  it should "work in custom situation" in {
    val start = new DateTime(2014, 12, 11, 16, 19)
    val end   = new DateTime(2015,  1,  8, 16, 19)
    val intervals = EstimateComputer.businessWeekIntervals(start, end)
    intervals shouldEqual List(
      new Interval(start,                            new DateTime(2014, 12, 13, 0, 0)),
      new Interval(new DateTime(2014, 12, 15, 0, 0), new DateTime(2014, 12, 20, 0, 0)),
      new Interval(new DateTime(2014, 12, 22, 0, 0), new DateTime(2014, 12, 27, 0, 0)),
      new Interval(new DateTime(2014, 12, 29, 0, 0), new DateTime(2015,  1,  3, 0, 0)),
      new Interval(new DateTime(2015,  1,  5, 0, 0), end)
    )
  }


}
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

import org.github.microburn.domain.ProjectConfig
import org.github.microburn.util.date.DateMath
import org.joda.time._

import scala.math.BigDecimal.RoundingMode
import scalaz.Scalaz._

object EstimateComputer {

  private final val COMPARING_SCALE = 1

  def estimatesBetween(start: DateTime, end: DateTime, storyPointsSum: BigDecimal)
                      (implicit config: ProjectConfig): Seq[Probe] = {
    val init = if (storyPointsSum == BigDecimal(0)) {
      IndexedSeq(Probe(start, BigDecimal(0)))
    } else {
      estimatesForNonZeroStoryPointsSum(start, end, storyPointsSum)
    }
    appendEndIfLastEstimateBeforeThem(init, end)
  }

  private def appendEndIfLastEstimateBeforeThem(init: IndexedSeq[Probe], end: DateTime): Seq[Probe] = {
    if (init.last.date != end)
      init :+ Probe(end, BigDecimal(0))
    else
      init
  }

  private def estimatesForNonZeroStoryPointsSum(start: DateTime, end: DateTime, storyPointsSum: BigDecimal)
                                               (implicit config: ProjectConfig): IndexedSeq[Probe] = {
    val intervalsAndSums = intervalAndSumMillisAfterThem(daysIntervals(start, end))
    val weightedSumOfIntervalsMillis = intervalsAndSums.lastOption.map(_.weightedSumAfter).getOrElse(BigDecimal(0))
    val steps = computeSteps(storyPointsSum)
    val computeWeightedMillis = millisForStoryPoints(weightedSumOfIntervalsMillis, storyPointsSum) _
    steps.map { storyPoints =>
      val date = momentInIntervals(intervalsAndSums, computeWeightedMillis(storyPoints))
      Probe(date, storyPoints)
    }.toIndexedSeq
  }

  private def intervalAndSumMillisAfterThem(intervals: List[Interval])
                                           (implicit config: ProjectConfig): Seq[IntervalAndSumMillis] = {
    intervals.tail.scanLeft(IntervalAndSumMillis(intervals.head, 0)) { (sum, nextInterval) =>
      IntervalAndSumMillis(nextInterval, sum.weightedSumAfter)
    }
  }

  private def daysIntervals(start: DateTime, end: DateTime): List[Interval] = {
    val startIntervalsStream: Stream[DateTime] = Stream.iterate(start) { prev =>
      prev.plusDays(1).withTimeAtStartOfDay()
    }
    val positiveIntervalsStream = startIntervalsStream.map { startOfInterval =>
      val endOfInterval = DateMath.minOfDates(end, startOfInterval.plusDays(1).withTimeAtStartOfDay())
      endOfInterval.isAfter(startOfInterval).option(new Interval(startOfInterval, endOfInterval))
    }
    positiveIntervalsStream.takeWhile(_.isDefined).map(_.get).toList
  }

  private def computeSteps(storyPointsSum: BigDecimal): Seq[BigDecimal] = {
    val additionalStepForNonWhole = (!storyPointsSum.isWhole()).option(storyPointsSum)
    additionalStepForNonWhole.toSeq ++ storyPointsSum.setScale(0, RoundingMode.FLOOR).to(0, step = -1)
  }

  private def millisForStoryPoints(weightedSumOfIntervalsMillis: BigDecimal, storyPointsSum: BigDecimal)
                                  (storyPoints: BigDecimal): BigDecimal = {
    weightedSumOfIntervalsMillis * (1 - storyPoints / storyPointsSum)
  }

  private def momentInIntervals(intervalsAndSums: Seq[IntervalAndSumMillis], weightedMillis: BigDecimal)
                               (implicit config: ProjectConfig): DateTime = {
    intervalsAndSums.find { interval =>
      val weightedMillisScaled = weightedMillis.setScale(COMPARING_SCALE, RoundingMode.DOWN)
      val weightedSumAfterScaled = interval.weightedSumAfter.setScale(COMPARING_SCALE, RoundingMode.UP)
      weightedMillisScaled <= weightedSumAfterScaled
    }.map { intervalAndSum =>
      intervalAndSum.dateAfter(weightedMillis - intervalAndSum.weightedSumBefore)
    } getOrElse { throw new IllegalArgumentException("Interval too short - cannot estimate") }
  }


  private[service] case class IntervalAndSumMillis(interval: Interval, weightedSumBefore: BigDecimal) {
    def weightedSumAfter(implicit config: ProjectConfig): BigDecimal = weightedSumBefore + weight * interval.toDurationMillis

    def dateAfter(weightedMillis: BigDecimal)
                 (implicit config: ProjectConfig): DateTime = {
      if (weightedMillis == BigDecimal(0)) // protect against 0 / 0 situation
        interval.getStart
      else
        interval.getStart.plusMillis((weightedMillis / weight).toInt)
    }
    
    private def weight(implicit config: ProjectConfig): BigDecimal = {
      config.dayOfWeekWeights(interval.getStart.getDayOfWeek - 1)
    }
  }
}

case class Probe(date: DateTime, sp: BigDecimal)

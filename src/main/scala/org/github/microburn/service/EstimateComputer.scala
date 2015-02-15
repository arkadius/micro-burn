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
import org.github.microburn.util.logging.Slf4jLogging
import org.joda.time._

import scala.collection.immutable.Seq
import scala.math.BigDecimal.RoundingMode
import scalaz.Scalaz._

object EstimateComputer extends Slf4jLogging {

  def estimatesBetween(start: DateTime, end: DateTime, storyPointsSum: BigDecimal)
                      (implicit config: ProjectConfig): List[Probe] = measure("estimates computation") {
    if (storyPointsSum == BigDecimal(0)) {
      List(
        Probe(start, BigDecimal(0)),
        Probe(end, BigDecimal(0))
      )
    } else {
      estimatesForNonZeroStoryPointsSum(start, end, storyPointsSum)
    }
  }

  private def estimatesForNonZeroStoryPointsSum(start: DateTime, end: DateTime, storyPointsSum: BigDecimal)
                                               (implicit config: ProjectConfig): List[Probe] = {
    val intervalsAndSums = intervalAndSumMillisAfterThem(daysIntervals(start, end))
    val weightedSumOfIntervalsMillis = intervalsAndSums.lastOption.map(_.weightedSumAfter).getOrElse(BigDecimal(0))
    val steps = computeSteps(storyPointsSum)
    val computeWeitghtedMillis = millisForStoryPoints(storyPointsSum, weightedSumOfIntervalsMillis) _
    steps.map { storyPoints =>
      val date = momentInIntervals(intervalsAndSums, computeWeitghtedMillis(storyPoints))
      Probe(date, storyPoints)
    }.toList
  }

  private def computeSteps(storyPointsSum: BigDecimal): scala.Seq[BigDecimal] = {
    val additionalStepForNonWhole = (!storyPointsSum.isWhole()).option(storyPointsSum)
    additionalStepForNonWhole.toSeq ++ storyPointsSum.setScale(0, RoundingMode.FLOOR).to(0, step = -1)
  }

  private def millisForStoryPoints(storyPointsSum: BigDecimal, weightedSumOfIntervalsMillis: BigDecimal)
                                  (storyPoints: BigDecimal): BigDecimal = {
    weightedSumOfIntervalsMillis * (1 - storyPoints / storyPointsSum)
  }

  private def daysIntervals(start: DateTime, end: DateTime): List[Interval] = {
    val startIntervalsStream: Stream[DateTime] = Stream.iterate(start) { prev =>
      prev.plusDays(1).withTimeAtStartOfDay()
    }
    val positiveIntervalsStream = startIntervalsStream.map { startOfInterval =>
      val endOfInterval = minOfDates(end, startOfInterval.plusDays(1).withTimeAtStartOfDay())
      endOfInterval.isAfter(startOfInterval).option(new Interval(startOfInterval, endOfInterval))
    }
    positiveIntervalsStream.takeWhile(_.isDefined).map(_.get).toList
  }

  private def intervalAndSumMillisAfterThem(intervals: List[Interval])
                                           (implicit config: ProjectConfig): Seq[IntervalAndSumMillis] = {
    intervals.tail.scanLeft(IntervalAndSumMillis(intervals.head, 0)) { (sum, nextInterval) =>
      IntervalAndSumMillis(nextInterval, sum.weightedSumAfter)
    }
  }

  private def momentInIntervals(intervalsAndSums: Seq[IntervalAndSumMillis], weightedMillis: BigDecimal)
                               (implicit config: ProjectConfig): DateTime = {
    intervalsAndSums.find(weightedMillis <= _.weightedSumAfter).map { intervalAndSum =>
      intervalAndSum.dateAfter(weightedMillis - intervalAndSum.weightedSumBefore)
    } getOrElse { throw new IllegalArgumentException("Interval too short - cannot estimate") }
  }

  private def minOfDates(first: DateTime, sec: DateTime): DateTime = {
    if (sec.isBefore(first))
      sec
    else
      first
  }

  private[service] case class IntervalAndSumMillis(interval: Interval, weightedSumBefore: BigDecimal) {
    def weightedSumAfter(implicit config: ProjectConfig): BigDecimal = weightedSumBefore + weight * interval.toDurationMillis

    def dateAfter(weightedMillis: BigDecimal)
                 (implicit config: ProjectConfig): DateTime = {
      interval.getStart.plusMillis((weightedMillis / weight).toInt)
    }
    
    private def weight(implicit config: ProjectConfig): BigDecimal = {
      config.dayOfWeekWeights(interval.getStart.getDayOfWeek - 1)
    }
  }
}

case class Probe(date: DateTime, sp: BigDecimal)

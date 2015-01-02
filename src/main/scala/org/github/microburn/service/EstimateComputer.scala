package org.github.microburn.service

import java.util.Date

import org.joda.time._

import scala.collection.immutable.Seq
import scalaz.Scalaz._
import scalaz._

object EstimateComputer {

  def estimatesBetween(start: DateTime, end: DateTime, storyPointsSum: Int): List[HistoryProbe] = {
    val intervalsAndSums = intervalAndSumMillisAfterThem(businessWeekIntervals(start, end))
    val sumOfIntervalsMillis = intervalsAndSums.lastOption.map(_.sumAfter).getOrElse(0L)
    storyPointsSum.to(0, step = -1).map { storyPoints =>
      val date = momentInIntervals(intervalsAndSums, (sumOfIntervalsMillis * (1 - storyPoints.toDouble / storyPointsSum)).toLong)
      HistoryProbe(date.getMillis, storyPoints)
    }.toList        
  }

  private[service] def businessWeekIntervals(start: DateTime, end: DateTime): List[Interval] = {
    val startIntervalsStream: Stream[DateTime] = Stream.iterate(start) { prev =>
      prev.plusWeeks(1).withDayOfWeek(DateTimeConstants.MONDAY).withTimeAtStartOfDay()
    }
    val withDroppedStartInWeekend = if (start.isBefore(start.withDayOfWeek(DateTimeConstants.SATURDAY).withTimeAtStartOfDay())) {
      startIntervalsStream
    } else {
      startIntervalsStream.drop(1)
    }
    val positiveIntervalsStream = withDroppedStartInWeekend.map { startOfInterval =>
      val endOfInterval = minOfDates(end, startOfInterval.withDayOfWeek(DateTimeConstants.SATURDAY).withTimeAtStartOfDay())
      endOfInterval.isAfter(startOfInterval).option(new Interval(startOfInterval, endOfInterval))
    }
    positiveIntervalsStream.takeWhile(_.isDefined).map(_.get).toList
  }

  private[service] def intervalAndSumMillisAfterThem(intervals: List[Interval]): Seq[IntervalAndSumMillis] = {
    lazy val intervalsStream: Stream[IntervalAndSumMillis] =
      IntervalAndSumMillis(intervals.head, 0) #::
        (intervalsStream zip intervals.tail).map {
          case (sum, nextInterval) =>
            IntervalAndSumMillis(nextInterval, sum.sumAfter)
        }
    intervalsStream.toList
  }

  private def momentInIntervals(intervalsAndSums: Seq[IntervalAndSumMillis], millis: Long): DateTime = {
    intervalsAndSums.find(millis <= _.sumAfter).map { intervalAndSum =>
      intervalAndSum.dateAfter(millis - intervalAndSum.sumBefore)
    } getOrElse { throw new IllegalArgumentException("Interval too short - cannot estimate") }
  }

  private def minOfDates(first: DateTime, sec: DateTime): DateTime = {
    if (sec.isBefore(first))
      sec
    else
      first
  }

  private[service] case class IntervalAndSumMillis(interval: Interval, sumBefore: Long) {
    def sumAfter: Long = sumBefore + interval.toDurationMillis
    
    def dateAfter(millis: Long): DateTime = {
      interval.getStart.plusMillis(millis.toInt)
    }
  }

}

package org.github.jiraburn.domain

import java.util.Date
import scalaz._
import Scalaz._

case class DateWithColumnsState(date: Date, indexOnSum: Map[Int, Int]) {
  def plus(otherIndexOnSum: Map[Int, Int]): DateWithColumnsState = {
    copy(indexOnSum = indexOnSum |+| otherIndexOnSum)
  }

  def multiply(const: Int): DateWithColumnsState = {
    copy(indexOnSum = indexOnSum.mapValues(_ * const))
  }

  def storyPointsForColumn(boardColumnIndex: Int) = indexOnSum.getOrElse(boardColumnIndex, 0)

  def nonEmpty = indexOnSum.values.exists(_ != 0)
}

object DateWithColumnsState {
  def zero(date: Date)(implicit config: ProjectConfig): DateWithColumnsState = DateWithColumnsState(date, constIndexOnSum(0))

  def constIndexOnSum(c: Int)(implicit config: ProjectConfig): Map[Int, Int] = config.boardColumns.map(_.index -> c).toMap
}
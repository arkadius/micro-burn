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
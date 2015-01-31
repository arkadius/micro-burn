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

import scalaz._
import Scalaz._

object StoryPointsFromName {
  private final val NAME_WITH_SP_PATTERN = "(\\(([\\d]*)\\))?\\s*(.*)".r("spWrappedByBraces", "nullableStoryPoints", "name")

  def unapply(nameWithOptionalSp: String): Option[(Option[Int], String)] =
    NAME_WITH_SP_PATTERN.unapplySeq(nameWithOptionalSp).map {
      case spWrappedByBraces :: nullableStoryPoints :: name :: Nil =>
        (Option(nullableStoryPoints).map(_.trim).filter(_.nonEmpty).map(_.toInt), name)
    }
}
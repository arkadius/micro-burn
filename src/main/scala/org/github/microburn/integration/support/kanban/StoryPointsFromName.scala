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

import scala.util.matching.Regex

object StoryPointsFromName {
  private final val NAME_WITH_SP_PATTERN: Regex = "\\(([\\d\\.]*)\\)\\s*(.*)".r("spInsideBraces", "name")

  private final val MAX_SCALE: Int = 3 // ten limit jest dlatego, że przy określonej precyzji źle zadziała round-trip dla zapisu-odczytu sp

  def unapply(nameWithOptionalSp: String): Option[(Option[BigDecimal], String)] = {
    val extracted = NAME_WITH_SP_PATTERN.unapplySeq(nameWithOptionalSp).flatMap {
      case spInsideBraces :: name :: Nil =>
        Option(spInsideBraces)
          .map(_.trim)
          .filter(_.nonEmpty)
          .map(BigDecimal(_))
          .filter(_.scale <= MAX_SCALE).map { storyPoints =>
          (Some(storyPoints), name)
        }
      case _ => None
    }.getOrElse {
      (None, nameWithOptionalSp)
    }
    Some(extracted)
  }
}
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
package org.github.microburn.domain.generator

import org.scalacheck.Gen

object StoryPointsGenerator {

  val spGenerator: Gen[BigDecimal] = Gen.posNum[Int].map { i => BigDecimal(i) / 10 }

  val optionalSpGenerator: Gen[Option[BigDecimal]] = Gen.option(spGenerator)

  def chooseAtMost(most: BigDecimal): Gen[BigDecimal] = Gen.chooseNum(0, (most * 10).toIntExact).map(BigDecimal(_) / 10)

}
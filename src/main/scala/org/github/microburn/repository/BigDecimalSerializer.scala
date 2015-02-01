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
package org.github.microburn.repository

import net.liftweb.json._

object BigDecimalSerializer extends Serializer[BigDecimal] {
  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), BigDecimal] = {
    case (TypeInfo(clazz, _), json) if classOf[BigDecimal].isAssignableFrom(clazz) => json match {
      case JDouble(xs) =>
        BigDecimal(xs)
      case JInt(xs) =>
        BigDecimal(xs)
      case JString(xs) =>
        BigDecimal(xs)
      case x => throw new MappingException("Can't convert " + x + " to BigDecimal")
    }
  }

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case i: BigDecimal if i.isWhole() => JInt(i.toBigInt())
    case nonWhole: BigDecimal => JDouble(nonWhole.toDouble)
  }
}
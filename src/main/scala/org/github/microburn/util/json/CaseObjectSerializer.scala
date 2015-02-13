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
package org.github.microburn.util.json

import net.liftweb.json._

case class CaseObjectSerializer[T <: AnyRef](obj: T) extends Serializer[T] {
  import CaseObjectSerializer._

  private val idValue = JString(obj.getClass.getName)

  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), T] = {
    case (_, JObject(List(JField(ID_FIELD, value)))) if value == idValue => obj
  }

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case `obj` =>
      JObject(List(JField(ID_FIELD, idValue)))
  }
}

object CaseObjectSerializer {
  private final val ID_FIELD = "jsonObject"

  def sequence[T <: AnyRef](objs: T*): Seq[CaseObjectSerializer[T]] = {
    objs.map(CaseObjectSerializer(_))
  }
}
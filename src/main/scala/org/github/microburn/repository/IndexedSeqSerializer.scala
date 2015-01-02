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

object IndexedSeqSerializer extends Serializer[IndexedSeq[_]] {
  def deserialize(implicit format: Formats) = {
    case (TypeInfo(clazz, ptype), json) if classOf[IndexedSeq[_]].isAssignableFrom(clazz) => json match {
      case JArray(xs) =>
        val t = ptype.getOrElse(throw new MappingException("parameterized type not known"))
        xs.map(x => Extraction.extract(x, TypeInfo(t.getActualTypeArguments()(0).asInstanceOf[Class[_]], None))).toIndexedSeq
      case x => throw new MappingException("Can't convert " + x + " to IndexedSeq")
    }
  }

  def serialize(implicit format: Formats) = {
    case i: IndexedSeq[_] => JArray(i.map(Extraction.decompose).toList)
  }
}
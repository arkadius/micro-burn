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

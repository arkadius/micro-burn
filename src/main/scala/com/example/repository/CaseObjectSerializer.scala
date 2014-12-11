package com.example.repository

import net.liftweb.json._
import CaseObjectSerializer._

class CaseObjectSerializer[T <: AnyRef](obj: T) extends Serializer[T] {
  val Identifier = JString(obj.getClass.getName)
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case `obj` =>
      JObject(List(JField(FieldKeyDefault, Identifier)))
  }
  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), T] = {
    case (_, JObject(List(JField(FieldKeyDefault, Identifier)))) => obj
  }
}

object CaseObjectSerializer {
  val FieldKeyDefault = "jsonObject"

  def sequence[T <: AnyRef](objs: T*): Seq[CaseObjectSerializer[T]] = {
    objs.map(new CaseObjectSerializer(_))
  }
}
package com.example.actors

import net.liftweb.actor.LAFuture

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object FutureEnrichments {

  implicit class LaFutureConvertibleToUtilConcurrentFuture[T](laFuture: LAFuture[T]) {
    def mapTo[TT: ClassTag]: LAFuture[TT] = {
      laFuture.map(_.asInstanceOf[TT])
    }

    def get(timeout: FiniteDuration) = laFuture.get(timeout.toMillis)
  }

}

package com.example.actors

import net.liftweb.actor.LAFuture

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object FutureEnrichments {

  implicit class EnrichedLAFuture[T](laFuture: LAFuture[T]) {
    def mapTo[TT: ClassTag]: LAFuture[TT] = {
      laFuture.map(_.asInstanceOf[TT])
    }

    def await(timeout: FiniteDuration): T =
      laFuture.get(timeout.toMillis).openOr(throw new TimeoutException(s"Timeout (after $timeout) awaiting for LAFuture result"))
  }

}

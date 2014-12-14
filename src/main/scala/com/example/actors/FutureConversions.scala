package com.example.actors

import net.liftweb.actor.LAFuture
import net.liftweb.common.{Empty, Full, Failure}

import scala.concurrent.{Future, Promise}

object FutureConversions {

  implicit class LaFutureConvertibleToUtilConcurrentFuture[T](laFuture: LAFuture[T]) {
    def toFuture: Future[T] = {
      val promise = Promise[T]()

      laFuture.onComplete {
        case Empty =>
          promise.failure(new Exception(s"Empty response"))
        case Full(value) =>
          promise.success(value)
        case Failure(msg, exceptionBox, _) =>
          promise.failure(exceptionBox.getOrElse(new Exception(s"Failure without exception with message: $msg")))
      }

      promise.future
    }
  }

}

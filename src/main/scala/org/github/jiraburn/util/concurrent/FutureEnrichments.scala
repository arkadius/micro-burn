package org.github.jiraburn.util.concurrent

import net.liftweb.actor.LAFuture
import net.liftweb.common.{Empty, Failure, Full}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}
import scala.reflect.ClassTag
import scala.util.Success

object FutureEnrichments {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class EnrichedLAFuture[T](laFuture: LAFuture[T]) {
    def mapTo[TT: ClassTag]: LAFuture[TT] = {
      laFuture.map(_.asInstanceOf[TT])
    }

    def await(timeout: FiniteDuration): T = {
      laFuture.get(timeout.toMillis) match {
        case Full(value) => value
        case Failure(msg, ex, _) => throw ex.openOr(new Exception(s"Failure: $msg"))
        case Empty => throw new TimeoutException(s"Timeout (after $timeout) awaiting for LAFuture result")
      }
    }
  }

  implicit class ScalaFutureConvertibleToLAFuture[T](scf: Future[T]) {
    def toLiftFuture: LAFuture[T] = {
      val laf = new LAFuture[T]
      scf.onComplete {
        case Success(v) => laf.satisfy(v)
        case scala.util.Failure(e) => laf.fail(Failure(e.getMessage, Full(e), Empty))
      }
      laf
    }
  }
}

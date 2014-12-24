package org.github.micoburn.util.concurrent

import net.liftweb.actor.LAFuture
import net.liftweb.common.{Empty, Failure, Full}
import org.github.micoburn.util.logging.Slf4jLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}
import scala.reflect.ClassTag
import scala.util.Success

object FutureEnrichments {
  import scala.concurrent.ExecutionContext.Implicits.global

  def collectWithWellEmptyListHandling[T](futures: Seq[LAFuture[T]]): LAFuture[List[T]] = {
    if (futures.nonEmpty)
      LAFuture.collect(futures : _*)
    else
      LAFuture[List[T]](() => Nil)
  }

  implicit class EnrichedLAFuture[T](laFuture: LAFuture[T]) extends Slf4jLogging {
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

    def withLoggingFinished(f: T => String) = {
      laFuture.map { result =>
        debug(s"Finished with ${f(result)}")
        result
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

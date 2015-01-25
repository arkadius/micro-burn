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
package org.github.microburn.util.concurrent

import net.liftweb.actor.LAFuture
import net.liftweb.common.{Empty, Failure, Full}
import org.github.microburn.util.logging.Slf4jLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}
import scala.reflect.ClassTag
import scala.util.Success

object FutureEnrichments {
  import scala.concurrent.ExecutionContext.Implicits.global

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
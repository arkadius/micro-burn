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
package org.github.microburn.util.logging

import net.liftweb.actor.LAFuture
import org.slf4j.LoggerFactory

trait Slf4jLogging {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def measure[T](taskName: => String)(runTask: => T): T = {
    val before = System.currentTimeMillis()
    val result = runTask
    debug(s"$taskName took ${System.currentTimeMillis()-before} ms")
    result
  }

  def measureFuture[T](taskName: => String)(prepareFuture: => LAFuture[T]): LAFuture[T] = {
    val before = System.currentTimeMillis()
    val resultFuture = prepareFuture
    resultFuture.onComplete { result =>
      debug(s"$taskName took ${System.currentTimeMillis()-before} ms")
    }
    resultFuture
  }
  
  def info(mes: => String) = if (logger.isInfoEnabled) logger.info(mes)
  def warn(mes: => String) = if (logger.isWarnEnabled) logger.warn(mes)
  def debug(mes: => String) = if (logger.isDebugEnabled) logger.debug(mes)
  def error(mes: => String) = if (logger.isErrorEnabled) logger.error(mes)
  def trace(mes: => String) = if (logger.isTraceEnabled) logger.trace(mes)

  def info(mes: => String, e: Throwable) = if (logger.isInfoEnabled) logger.info(mes, e)
  def warn(mes: => String, e: Throwable) = if (logger.isWarnEnabled) logger.warn(mes, e)
  def debug(mes: => String, e: Throwable) = if (logger.isDebugEnabled) logger.debug(mes, e)
  def error(mes: => String, e: Throwable) = if (logger.isErrorEnabled) logger.error(mes, e)
  def trace(mes: => String, e: Throwable) = if (logger.isTraceEnabled) logger.trace(mes, e)
}
package org.github.micoburn.util.logging

import org.slf4j.LoggerFactory

trait Slf4jLogging extends Logging {
  private lazy val logger = LoggerFactory.getLogger(getClass)

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


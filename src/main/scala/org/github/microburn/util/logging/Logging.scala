package org.github.microburn.util.logging

trait Logging {
  def info(mes: =>String)
  def warn(mes: =>String)
  def debug(mes: =>String)
  def error(mes: =>String)
  def trace(mes: =>String)

  def info(mes: =>String, e:Throwable)
  def warn(mes: =>String, e:Throwable)
  def debug(mes: =>String, e:Throwable)
  def error(mes: =>String, e:Throwable)
  def trace(mes: =>String, e:Throwable)
}

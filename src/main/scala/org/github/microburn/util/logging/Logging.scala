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
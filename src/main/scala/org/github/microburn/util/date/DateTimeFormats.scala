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
package org.github.microburn.util.date

import java.text.SimpleDateFormat
import java.util.TimeZone

object DateTimeFormats {

  def utcFullDateTimeFormat = {
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    f.setTimeZone(TimeZone.getTimeZone("UTC"))
    f
  }
  
  def utcUnderscoredDateTimeFormat = {
    val f = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss'Z'")
    f.setTimeZone(TimeZone.getTimeZone("UTC"))
    f
  }

  def localDateFormat = {
    new SimpleDateFormat("yyyy-MM-dd")
  }
}
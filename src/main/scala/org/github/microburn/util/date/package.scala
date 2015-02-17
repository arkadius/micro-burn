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
package org.github.microburn.util

import java.text.SimpleDateFormat
import java.util.TimeZone

import org.joda.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}

package object date {

  def utcDateFormat = {
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    f.setTimeZone(TimeZone.getTimeZone("UTC"))
    f
  }

  val dateTimeFormatterWithOptionalTimeFields: DateTimeFormatter = {
    val secondParser = new DateTimeFormatterBuilder()
      .appendLiteral(':')
      .appendSecondOfMinute(1)
      .toParser

    val minuteParser = new DateTimeFormatterBuilder()
      .appendLiteral(':')
      .appendMinuteOfHour(1)
      .appendOptional(secondParser)
      .toParser

    val hourParser = new DateTimeFormatterBuilder()
      .appendLiteral(' ')
      .appendHourOfDay(1)
      .appendOptional(minuteParser)
      .toParser

    new DateTimeFormatterBuilder()
      .appendYear(4, 4)
      .appendLiteral('-')
      .appendMonthOfYear(1)
      .appendLiteral('-')
      .appendDayOfMonth(1)
      .appendOptional(hourParser)
      .toFormatter
  }

}
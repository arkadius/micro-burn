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
package org.github.microburn.util.config

import com.typesafe.config.{Config, ConfigException}
import org.joda.time.{DateTime, DateTimeConstants}
import org.github.microburn.util.date._

import scalaz.Scalaz._

object ConfigExtensions {
  import scala.collection.convert.wrapAsScala._

  implicit class ConfigExtension(config: Config) {
    def optional[T](f: Config => String => T, path: String): Option[T] = {
      config.hasPath(path).option(f(config)(path))
    }

    def getDefinedBoolean(path: String, notDefinedDefault: => Boolean = false): Boolean = {
      config.optional(_.getBoolean, path).getOrElse(notDefinedDefault)
    }

    def getBigDecimal(path: String): BigDecimal = {
      BigDecimal(config.getNumber(path).toString)
    }

    def getBigDecimalList(path: String): List[BigDecimal] = {
      config.getNumberList(path).map(n => BigDecimal(n.toString)).toList
    }

    def getDayOfWeek(path: String): Int = config.getString(path) match {
      case "monday"     | "mon" | "mo" => DateTimeConstants.MONDAY
      case "tuesday"    | "tue" | "tu" => DateTimeConstants.TUESDAY
      case "wednesday"  | "wed" | "we" => DateTimeConstants.WEDNESDAY
      case "thursday"   | "thu" | "th" => DateTimeConstants.THURSDAY
      case "friday"     | "fri" | "fr" => DateTimeConstants.FRIDAY
      case "saturday"   | "sat" | "sa" => DateTimeConstants.SATURDAY
      case "sunday"     | "sun" | "su" => DateTimeConstants.SUNDAY
      case other => throw new ConfigException.BadValue(path, s"Not supported day of week: $other")
    }

    def getDateTime(path: String): DateTime = {
      val str = config.getString(path)
      try {
        dateTimeFormatterWithOptionalTimeFields.parseDateTime(str)
      } catch {
        case ex: IllegalArgumentException =>
          throw new ConfigException.BadValue(path, s"Bad date: $str. Should be in format: yyyy-MM-dd HH:mm:ss. Time is optional", ex)
      }
    }
  }
}
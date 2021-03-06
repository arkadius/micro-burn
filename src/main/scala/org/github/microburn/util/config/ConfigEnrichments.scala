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

import java.text.ParseException

import com.typesafe.config.{Config, ConfigException}
import org.github.microburn.util.date._
import org.joda.time.{DateTime, DateTimeConstants}

import scalaz.Scalaz._

object ConfigEnrichments {
  import scala.collection.convert.wrapAsScala._

  implicit class EnrichedConfig(config: Config) {
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

    def getDayOfMonth(path: String): Int = {
      val i = config.getInt(path)
      if (1 <= i && i <= 31)
        i
      else
        throw new ConfigException.BadValue(path, "Day of month should be between 1 and 31")
    }

    def getTime(path: String): Time = {
      TimeParser.parse(config.getString(path)).valueOr(msg => throw new ConfigException.BadValue(path, msg))
    }

    def getDayOfWeek(path: String): Int = config.getString(path) match {
      case "monday"     | "mon" | "mo" => DateTimeConstants.MONDAY
      case "tuesday"    | "tue" | "tu" => DateTimeConstants.TUESDAY
      case "wednesday"  | "wed" | "we" => DateTimeConstants.WEDNESDAY
      case "thursday"   | "thu" | "th" => DateTimeConstants.THURSDAY
      case "friday"     | "fri" | "fr" => DateTimeConstants.FRIDAY
      case "saturday"   | "sat" | "sa" => DateTimeConstants.SATURDAY
      case "sunday"     | "sun" | "su" => DateTimeConstants.SUNDAY
      case other => throw new ConfigException.BadValue(path, s"Invalid day of week: $other")
    }

    def getDate(path: String): DateTime = {
      val str = config.getString(path)
      try {
        new DateTime(DateTimeFormats.localDateFormat.parse(str))
      } catch {
        case ex: ParseException =>
          throw new ConfigException.BadValue(path, s"Invalid date: $str. Should be in format: yyyy-MM-dd", ex)
      }
    }
  }
}
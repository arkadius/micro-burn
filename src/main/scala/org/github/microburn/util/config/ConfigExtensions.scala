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

import com.typesafe.config.Config

import scalaz._
import Scalaz._

object ConfigExtensions {
  import collection.convert.wrapAsScala._

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
  }
}
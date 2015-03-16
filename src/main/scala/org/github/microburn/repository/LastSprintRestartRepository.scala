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
package org.github.microburn.repository

import java.io.{FileWriter, File}

import org.joda.time.DateTime
import scala.io.Source
import scalaz.Scalaz._
import org.github.microburn.util.date._

trait LastSprintRestartRepository {
  def loadLastSprintRestart: Option[DateTime]
  def saveLastSprintRestart(dateTime: DateTime): Unit
}

object LastSprintRestartRepository {
  def apply(projectRoot: File): LastSprintRestartRepository = new LastSprintRestartTxtRepository(projectRoot)
}

class LastSprintRestartTxtRepository(projectRoot: File) extends LastSprintRestartRepository {
  override def loadLastSprintRestart: Option[DateTime] = {
    val file = new File(projectRoot, "lastSprintRestart.txt")
    file.exists().option {
      new DateTime(DateTimeFormats.utcDateTimeFormat.parse(Source.fromFile(file).mkString.trim))
    }
  }

  override def saveLastSprintRestart(dateTime: DateTime): Unit = {
    projectRoot.mkdirs()
    val file = new File(projectRoot, "lastSprintRestart.txt")
    val writer = new FileWriter(file)
    try {
      writer.write(DateTimeFormats.utcDateTimeFormat.format(dateTime.toDate) + "\n")
    } finally {
      writer.close()
    }
  }
}
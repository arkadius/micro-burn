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

import java.io.{File, PrintWriter}
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import org.github.microburn.domain._

import scala.io.Source
import scala.util.control.Exception._

trait BoardStateRepository {
  def saveCurrentUserStories(sprint: Sprint): Unit

  def loadInitialUserStories: Option[BoardState]
  def loadCurrentUserStories: Option[BoardState]

  def cleanUnnecessaryStates(): Unit
}

object BoardStateRepository {
  def apply(sprintRoot: File): BoardStateRepository = {
    new BoardStateFSRepository(sprintRoot)
  }
}

class BoardStateFSRepository(sprintRoot: File) extends BoardStateRepository {
  import net.liftweb.json.Extraction._
  import net.liftweb.json._

  implicit val formats = DefaultFormats.withHints(FullTypeHints(List(classOf[Task], classOf[TechnicalTask]))) + IndexedSeqSerializer

  override def saveCurrentUserStories(sprint: Sprint): Unit = {
    sprintRoot.mkdirs()
    val sprintJsonFile = new File(sprintRoot, jsonFileName(sprint.currentBoard.date))
    val rendered = pretty(render(decompose(sprint.currentBoard.userStories)))
    val writer = new PrintWriter(sprintJsonFile)
    try {
      writer.write(rendered)
    } finally {
      writer.close()
    }
  }

  private def jsonFileName(date: Date) = dateFormat.format(date) + ".json"

  override def loadInitialUserStories: Option[BoardState] = {
    sortedJsonFiles.headOption.map(loadUserStories _ tupled)
  }

  override def loadCurrentUserStories: Option[BoardState] = {
    sortedJsonFiles.lastOption.map(loadUserStories _ tupled)
  }

  private def loadUserStories(sprintJsonFile: File, date: Date): BoardState = {
    val content = Source.fromFile(sprintJsonFile).mkString
    BoardState(extract[Array[UserStory]](parse(content)).toIndexedSeq, date)
  }

  override def cleanUnnecessaryStates(): Unit = {
    sortedJsonFiles.drop(1).dropRight(1).foreach {
      case (file, date) =>
        file.delete()
    }
  }

  private def sortedJsonFiles: Seq[(File, Date)] = {
    val jsonFilesWithDates = for {
      files <- Option(sprintRoot.listFiles()).toSeq
      file <- files
      if file.getName.endsWith(".json")
      date <- parseFileName(file)
    } yield (file, date)
    jsonFilesWithDates.sortBy {
      case (file, date) => date
    }
  }

  private def parseFileName(file: File): Option[Date] = {
    catching(classOf[ParseException]) opt {
      dateFormat.parse(file.getName.replaceAll(".json", ""))
    }
  }

  private def dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
}
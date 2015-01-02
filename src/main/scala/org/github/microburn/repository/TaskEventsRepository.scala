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

import java.io.File
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import com.github.tototoshi.csv._
import org.github.microburn.domain.{TaskUpdated, TaskRemoved, TaskAdded, TaskEvent}

import scala.util.control.NonFatal
import scalaz.Scalaz._
import scalaz._

trait TaskEventsRepository {
  def appendTasksEvents(events: Seq[TaskEvent]): Unit

  def loadTaskEvents: Seq[TaskEvent]
}

object TaskEventsRepository {
  def apply(sprintRoot: File): TaskEventsRepository = {
    new TaskEventsCsvRepository(new File(sprintRoot, "taskEvents.csv"))
  }
}

class TaskEventsCsvRepository(taskEventsFile: File) extends TaskEventsRepository {

  private final val ADDED   = "added"
  private final val UPDATED = "updated"
  private final val REMOVED = "removed"

  private val csvFormat = new CSVFormat {
    override val delimiter: Char = ';'
    override val quoteChar: Char = '"'
    override val treatEmptyLineAsNil: Boolean = false
    override val escapeChar: Char = '\\'
    override val lineTerminator: String = "\n"
    override val quoting: Quoting = QUOTE_ALL
  }

  private val header    = Seq("operation", "taskId", "parentUserStoryId", "isTechnicalTask", "taskName", "optionalStoryPoints", "status", "date")

  private def toFields(event: TaskEvent): Seq[Any] = event match {
    case e:TaskAdded   => Seq(ADDED,       e.taskId, e.parentUserStoryId, e.isTechnicalTask, e.taskName, e.optionalStoryPoints, e.status, e.date)
    case e:TaskUpdated => Seq(UPDATED,     e.taskId, e.parentUserStoryId, e.isTechnicalTask, e.taskName, e.optionalStoryPoints, e.status, e.date)
    case e:TaskRemoved => Seq(REMOVED,     e.taskId, e.parentUserStoryId, e.isTechnicalTask, "",         "",                    "",       e.date)
  }

  private def parseFields(fields: IndexedSeq[String]): TaskEvent = {
    val taskId              = fields(1)
    val parentUserStoryId   = fields(2)
    val isTechnicalTask     = fields(3).toBoolean
    def taskName            = fields(4)
    def optionalStoryPoints = parseOptionalInt(fields(5))
    def status              = fields(6).toInt
    val date                = dateFormat.parse(fields(7))
    fields(0) match {
      case ADDED   => TaskAdded(taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask,
        taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status, date = date)
      case UPDATED => TaskUpdated(taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask,
        taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status, date = date)
      case REMOVED => TaskRemoved(taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask, date = date)
      case otherType => throw new ParseException(s"Invalid event type: $otherType", -1)
    }
  }

  private def parseOptionalInt(str: String): Option[Int] =  str.nonEmpty.option(str.toInt)

  private def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")

  override def appendTasksEvents(events: Seq[TaskEvent]): Unit = {
    val csv = prepareWriter()
    try {
      events.foreach { event =>
        csv.writeRow(prepareFields(toFields(event)))
      }
    } finally {
      csv.close()
    }
  }

  private def prepareWriter(): CSVWriter = {
    if (!taskEventsFile.exists()) {
      taskEventsFile.getParentFile.mkdirs()
      taskEventsFile.createNewFile()
      val csv = CSVWriter.open(taskEventsFile)(csvFormat)
      try {
        csv.writeRow(header)
        csv
      } catch {
        case NonFatal(ex) =>
          csv.close()
          throw ex
      }
    } else {
      CSVWriter.open(taskEventsFile, append = true)(csvFormat)
    }
  }

  private def prepareFields(fields: Seq[Any]): Seq[Any] = fields.map {
    case date: Date => dateFormat.format(date)
    case optional: Option[_] => optional.getOrElse("")
    case other => other
  }

  override def loadTaskEvents: Seq[TaskEvent] =
    if (!taskEventsFile.exists()) {
      IndexedSeq.empty
    } else {
      val csv = CSVReader.open(taskEventsFile)(csvFormat)
      try {
        csv.toStream().drop(1).map { rawFields =>
          parseFields(rawFields.toIndexedSeq)
        }.toIndexedSeq
      } finally {
        csv.close()
      }
    }
}
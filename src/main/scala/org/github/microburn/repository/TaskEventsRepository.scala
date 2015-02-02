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
import java.text.ParseException
import java.util.Date

import com.github.tototoshi.csv._
import org.github.microburn.domain._
import org.github.microburn.util.date._

import scala.util.control.NonFatal
import scalaz.Scalaz._

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

  private final val SPECIFIED_STATUS = "specified"
  private final val COMPLETED_STATUS = "completed"

  private val csvFormat = new CSVFormat {
    override val delimiter: Char = ';'
    override val quoteChar: Char = '"'
    override val treatEmptyLineAsNil: Boolean = false
    override val escapeChar: Char = '\\'
    override val lineTerminator: String = "\n"
    override val quoting: Quoting = QUOTE_ALL
  }

  private val header    = Seq("operation",           "taskId",     "parentUserStoryId", "isTechnicalTask", "taskName",
                              "optionalStoryPoints", "statusType", "statusValue",       "date")

  private def toFields(event: TaskEvent): Seq[Any] = event match {
    case e:TaskAdded   => Seq(ADDED,                 e.taskId,     e.parentUserStoryId, e.isTechnicalTask, e.taskName,
                              e.optionalStoryPoints, t(e.status),  v(e.status),         e.date)
    case e:TaskUpdated => Seq(UPDATED,               e.taskId,     e.parentUserStoryId, e.isTechnicalTask, e.taskName,
                              e.optionalStoryPoints, t(e.status),  v(e.status),         e.date)
    case e:TaskRemoved => Seq(REMOVED,               e.taskId,     e.parentUserStoryId, e.isTechnicalTask, "",
                              "",                    "",           "",                  e.date)
  }

  private def parseFields(fields: IndexedSeq[String]): TaskEvent = {
    val taskId              = fields(1)
    val parentUserStoryId   = fields(2)
    val isTechnicalTask     = fields(3).toBoolean
    def taskName            = fields(4)
    def optionalStoryPoints = parseOptionalDecimal(fields(5))
    def status              = parseStatus(fields(6), fields(7))
    val date                = utcDateFormat.parse(fields(8))
    fields(0) match {
      case ADDED   => TaskAdded(taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask,
        taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status, date = date)
      case UPDATED => TaskUpdated(taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask,
        taskName = taskName, optionalStoryPoints = optionalStoryPoints, status = status, date = date)
      case REMOVED => TaskRemoved(taskId = taskId, parentUserStoryId = parentUserStoryId, isTechnicalTask = isTechnicalTask, date = date)
      case otherType => throw new ParseException(s"Invalid event type: $otherType", -1)
    }
  }

  private def t(status: TaskStatus): String = status match {
    case SpecifiedStatus(_)  => SPECIFIED_STATUS
    case TaskCompletedStatus => COMPLETED_STATUS
  }

  private def v(status: TaskStatus): String = status match {
    case SpecifiedStatus(value) => value
    case TaskCompletedStatus          => ""
  }

  private def parseStatus(typ: String, value: String): TaskStatus = typ match {
    case SPECIFIED_STATUS => SpecifiedStatus(value)
    case COMPLETED_STATUS => TaskCompletedStatus
    case otherStatusType => throw new ParseException(s"Invalid status type: $otherStatusType", -1)
  }


  private def parseOptionalDecimal(str: String): Option[BigDecimal] =  try {
    str.nonEmpty.option(BigDecimal(str))
  } catch {
    case e: NumberFormatException => throw new NumberFormatException(s"Cannot parse $str to decimal")
  }

  private def prepareFields(fields: Seq[Any]): Seq[Any] = fields.map {
    case date: Date => utcDateFormat.format(date)
    case optional: Option[_] => optional.getOrElse("")
    case other => other
  }

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
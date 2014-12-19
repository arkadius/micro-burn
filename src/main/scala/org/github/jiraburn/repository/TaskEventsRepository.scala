package org.github.jiraburn.repository

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.github.tototoshi.csv._
import org.github.jiraburn.domain.{TaskState, TaskChanged}

import scala.util.control.NonFatal
import scalaz._
import Scalaz._

trait TaskEventsRepository {
  def appendTasksEvents(events: Seq[TaskChanged]): Unit

  def loadTaskEvents: Seq[TaskChanged]
}

object TaskEventsRepository {
  def apply(sprintRoot: File): TaskEventsRepository = {
    new TaskEventsCsvRepository(new File(sprintRoot, "taskEvents.csv"))
  }
}

class TaskEventsCsvRepository(taskEventsFile: File) extends TaskEventsRepository {

  private val csvFormat = new CSVFormat {
    override val delimiter: Char = ';'
    override val quoteChar: Char = '"'
    override val treatEmptyLineAsNil: Boolean = false
    override val escapeChar: Char = '\\'
    override val lineTerminator: String = "\n"
    override val quoting: Quoting = QUOTE_ALL
  }

  override def appendTasksEvents(events: Seq[TaskChanged]): Unit = {
    val csv = prepareWriter()
    try {
      events.foreach { event =>
        csv.writeRow(prepareFields(event.productIterator.toList))
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
        csv.writeRow(Seq("taskId", "parentTaskId", "isTechnicalTask", "optionalFromStatus", "optionalFromStoryPoints", "optionalToStatus", "optionalToStoryPoints", "date"))
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

  private def prepareFields(fields: List[Any]): List[Any] = fields.flatMap {
    case date: Date => Seq(dateFormat.format(date))
    case optional: Option[_] =>
      optional.asInstanceOf[Option[TaskState]]
        .map(_.productIterator)
        .getOrElse(List("", ""))
    case other => Seq(other)
  }

  override def loadTaskEvents: Seq[TaskChanged] =
    if (!taskEventsFile.exists()) {
      Nil
    } else {
      val csv = CSVReader.open(taskEventsFile)(csvFormat)
      try {
        csv.toStream().drop(1).map { rawFields =>
          TaskChanged(
            taskId            = rawFields(0),
            parentTaskId      = rawFields(1),
            isTechnicalTask   = rawFields(2).toBoolean,
            optionalFromState = parseOptionalStatus(rawFields(3), rawFields(4)),
            optionalToState   = parseOptionalStatus(rawFields(5), rawFields(6)),
            date              = dateFormat.parse(rawFields(7))
          )
        }.toIndexedSeq
      } finally {
        csv.close()
      }
    }

  private def parseOptionalStatus(statusStr: String, storyPointsStr: String): Option[TaskState] = {
    val trimmedStatus = statusStr.trim
    val trimmedStoryPoints = storyPointsStr.trim
    (trimmedStatus.nonEmpty && trimmedStoryPoints.nonEmpty).option {
      TaskState(trimmedStatus.toInt, trimmedStoryPoints.toInt)
    }
  }

  private def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
}
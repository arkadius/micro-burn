package com.example.repository

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.example.domain.{TaskCompleted, TaskEvent, TaskReopened}
import com.github.tototoshi.csv._
import com.typesafe.config.Config

import scala.util.control.NonFatal

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

  private val csvFormat = new CSVFormat {
    override val delimiter: Char = ';'
    override val quoteChar: Char = '"'
    override val treatEmptyLineAsNil: Boolean = false
    override val escapeChar: Char = '\\'
    override val lineTerminator: String = "\n"
    override val quoting: Quoting = QUOTE_ALL
  }

  override def appendTasksEvents(events: Seq[TaskEvent]): Unit = {
    val csv = prepareWriter()
    try {
      events.foreach { event =>
        val fields = event match {
          case completed: TaskCompleted => true  :: TaskCompleted.unapply(completed).get.productIterator.toList
          case reopened: TaskReopened   => false :: TaskReopened.unapply(reopened).get.productIterator.toList
        }
        csv.writeRow(prepareFields(fields))
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
        csv.writeRow(Seq("completed", "taskId", "parentTaskId", "taskFromInitialScope", "date", "storyPoints"))
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

  private def prepareFields(fields: List[Any]): List[Any] = fields.map {
    case date: Date => dateFormat.format(date)
    case other => other
  }

  override def loadTaskEvents: Seq[TaskEvent] =
    if (!taskEventsFile.exists()) {
      Nil
    } else {
      val csv = CSVReader.open(taskEventsFile)(csvFormat)
      try {
        csv.toStream().drop(1).map { rawFields =>
          val completedEvent = rawFields(0).toBoolean
          val t = (
            rawFields(1),
            rawFields(2),
            rawFields(3).toBoolean,
            dateFormat.parse(rawFields(4)),
            rawFields(5).toInt
            )
          if (completedEvent)
            (TaskCompleted.apply _ tupled)(t)
          else
            (TaskReopened.apply _ tupled)(t)
        }.toIndexedSeq
      } finally {
        csv.close()
      }
    }

  private def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
}
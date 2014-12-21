package org.github.jiraburn.repository

import java.io.{File, PrintWriter}
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import org.github.jiraburn.domain._

import scala.io.Source
import scala.util.control.Exception._

trait SprintScopeRepository {
  def saveCurrentUserStories(sprint: Sprint): Unit

  def loadInitialUserStories: Option[SprintState]
  def loadCurrentUserStories: Option[SprintState]

  def cleanUnnecessaryStates(): Unit
}

object SprintScopeRepository {
  def apply(sprintRoot: File): SprintScopeRepository = {
    new SprintScopeFSRepository(sprintRoot)
  }
}

class SprintScopeFSRepository(sprintRoot: File) extends SprintScopeRepository {
  import net.liftweb.json.Extraction._
  import net.liftweb.json._

  implicit val formats = DefaultFormats.withHints(FullTypeHints(List(classOf[Task], classOf[TechnicalTask])))

  override def saveCurrentUserStories(sprint: Sprint): Unit = {
    sprintRoot.mkdirs()
    val sprintJsonFile = new File(sprintRoot, jsonFileName(sprint.currentState.date))
    val rendered = pretty(render(decompose(sprint.currentState.userStories)))
    val writer = new PrintWriter(sprintJsonFile)
    try {
      writer.write(rendered)
    } finally {
      writer.close()
    }
  }

  private def jsonFileName(date: Date) = dateFormat.format(date) + ".json"

  override def loadInitialUserStories: Option[SprintState] = {
    sortedJsonFiles.headOption.map(loadUserStories _ tupled)
  }

  override def loadCurrentUserStories: Option[SprintState] = {
    sortedJsonFiles.lastOption.map(loadUserStories _ tupled)
  }

  private def loadUserStories(sprintJsonFile: File, date: Date): SprintState = {
    val content = Source.fromFile(sprintJsonFile).mkString
    SprintState(extract[Array[UserStory]](parse(content)), date)
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
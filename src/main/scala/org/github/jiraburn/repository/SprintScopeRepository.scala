package org.github.jiraburn.repository

import java.io.{File, PrintWriter}
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import org.github.jiraburn.domain._

import scala.io.Source

trait SprintScopeRepository {
  def saveCurrentUserStories(sprint: Sprint)(timestamp: Date): Unit

  def loadInitialUserStories: Option[Seq[UserStory]]
  def loadCurrentUserStories: Option[Seq[UserStory]]

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

import scala.util.control.Exception._

  implicit val formats = DefaultFormats
    .withHints(FullTypeHints(List(classOf[Task], classOf[TechnicalTask])))

  override def saveCurrentUserStories(sprint: Sprint)(timestamp: Date): Unit = {
    sprintRoot.mkdirs()
    val sprintJsonFile = new File(sprintRoot, jsonFileName(timestamp))
    val rendered = pretty(render(decompose(sprint.currentUserStories)))
    val writer = new PrintWriter(sprintJsonFile)
    try {
      writer.write(rendered)
    } finally {
      writer.close()
    }
  }

  private def jsonFileName(date: Date) = dateFormat.format(date) + ".json"

  override def loadInitialUserStories: Option[Seq[UserStory]] = {
    sortedJsonFiles.headOption.map(loadUserStories)
  }

  override def loadCurrentUserStories: Option[Seq[UserStory]] = {
    sortedJsonFiles.lastOption.map(loadUserStories)
  }

  private def loadUserStories(sprintJsonFile: File): Seq[UserStory] = {
    val content = Source.fromFile(sprintJsonFile).mkString
    extract[Array[UserStory]](parse(content))
  }

  override def cleanUnnecessaryStates(): Unit = {
    sortedJsonFiles.drop(1).dropRight(1).foreach(_.delete())
  }

  private def sortedJsonFiles: Seq[File] = {
    val jsonFilesWithDates = for {
      files <- Option(sprintRoot.listFiles()).toSeq
      file <- files
      if file.getName.endsWith(".json")
      date <- parseFileName(file)
    } yield (file, date)
    jsonFilesWithDates.sortBy {
      case (file, date) => date
    } map {
      case (file, date) => file
    }
  }

  private def parseFileName(file: File): Option[Date] = {
    catching(classOf[ParseException]) opt {
      dateFormat.parse(file.getName.replaceAll(".json", ""))
    }
  }

  private def dateFormat = new SimpleDateFormat("yyyy_MM_dddd_HH_mm_ss")
}
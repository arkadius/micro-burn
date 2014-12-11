package com.example.repository

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date

import com.example.domain._
import com.typesafe.config.Config

import scala.io.Source

trait SprintScopeRepository {
  def saveUserStories(sprint: Sprint)(timestamp: Date): Unit

  def loadInitialUserStories(sprintId: String): Option[Seq[UserStory]]
  def loadCurrentUserStories(sprintId: String): Option[Seq[UserStory]]
}

object SprintScopeRepository {
  def apply(config: Config): SprintScopeRepository = {
    val sprintsRootPath = config.getString("data.sprints.root")
    new SprintScopeFSRepository(new File(sprintsRootPath))
  }
}

class SprintScopeFSRepository(sprintsRoot: File) extends SprintScopeRepository {
  import net.liftweb.json.Extraction._
  import net.liftweb.json._

  implicit val formats = DefaultFormats
    .withHints(FullTypeHints(List(classOf[Task], classOf[TechnicalTask]))) ++
    CaseObjectSerializer.sequence(Opened, Completed)

  override def saveUserStories(sprint: Sprint)(timestamp: Date): Unit = {
    val sprintRootDir = sprintRoot(sprint.id)
    sprintRootDir.mkdirs()
    val sprintJsonFile = new File(sprintRootDir, jsonFileName(timestamp))
    val rendered = pretty(render(decompose(sprint.currentUserStories)))
    val writer = new PrintWriter(sprintJsonFile)
    try {
      writer.write(rendered)
    } finally {
      writer.close()
    }
  }

  private def jsonFileName(date: Date) = dateFormat.format(date) + ".json"

  def loadInitialUserStories(sprintId: String): Option[Seq[UserStory]] = {
    sortedFiles(sprintId).headOption.map(loadUserStories)
  }

  def loadCurrentUserStories(sprintId: String): Option[Seq[UserStory]] = {
    sortedFiles(sprintId).lastOption.map(loadUserStories)
  }

  private def sortedFiles(sprintId: String): Seq[File] = {
    sprintRoot(sprintId).listFiles().filter(_.getName.endsWith(".json")).sortBy(parseFileName)
  }

  private def parseFileName(file: File): Date = {
    dateFormat.parse(file.getName.replaceAll(".json", ""))
  }

  private def dateFormat = new SimpleDateFormat("yyyy_MM_dddd_HH_mm_ss")

  private def sprintRoot(sprintId: String): File = {
    new File(sprintsRoot, sprintId)
  }

  private def loadUserStories(sprintJsonFile: File): Seq[UserStory] = {
    val content = Source.fromFile(sprintJsonFile).mkString
    extract[Array[UserStory]](parse(content))
  }
}
package com.example.repository

import java.io.{File, PrintWriter}

import com.example.domain._
import com.typesafe.config.Config

import scala.io.Source

trait SprintScopeRepository {
  def saveUserStories(sprint: Sprint): Unit

  def loadUserStories(sprintId: String): Seq[UserStory]
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

  implicit val formats = DefaultFormats.withHints(FullTypeHints(List(classOf[Task], classOf[TechnicalTask], classOf[TaskState])))

  override def saveUserStories(sprint: Sprint): Unit = {
    sprintsRoot.mkdir()
    val sprintJsonFile = jsonFile(sprint.id)
    val rendered = pretty(render(decompose(sprint.currentUserStories)))
    val writer = new PrintWriter(sprintJsonFile)
    try {
      writer.write(rendered)
    } finally {
      writer.close()
    }
  }

  override def loadUserStories(sprintId: String): Seq[UserStory] = {
    val sprintJsonFile = jsonFile(sprintId)
    val content = Source.fromFile(sprintJsonFile).mkString
    extract[Array[UserStory]](parse(content))
  }

  private def jsonFile(sprintId: String): File = {
    new File(sprintsRoot, sprintId + ".json")
  }
}
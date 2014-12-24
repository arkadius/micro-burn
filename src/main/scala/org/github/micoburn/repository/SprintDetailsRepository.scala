package org.github.micoburn.repository

import java.io.{PrintWriter, File}

import org.github.micoburn.domain._

import scala.io.Source

trait SprintDetailsRepository {
  def saveDetails(sprint: Sprint)
  
  def loadDetails: Option[SprintDetails]
}

object SprintDetailsRepository {
  def apply(sprintRoot: File): SprintDetailsRepository = new SprintDetailsJsonRepository(sprintRoot)
}

class SprintDetailsJsonRepository(sprintRoot: File) extends SprintDetailsRepository {
  import net.liftweb.json.Extraction._
  import net.liftweb.json._

  implicit val formats = DefaultFormats.withHints(FullTypeHints(List(classOf[SprintDetails])))

  override def saveDetails(sprint: Sprint): Unit = {
    sprintRoot.mkdirs()
    val detailsFile = new File(sprintRoot, "details.json")
    val rendered = pretty(render(decompose(sprint.details)))
    val writer = new PrintWriter(detailsFile)
    try {
      writer.write(rendered)
    } finally {
      writer.close()
    }
  }

  override def loadDetails: Option[SprintDetails] = {
    val detailsFile = new File(sprintRoot, "details.json")
    if (!detailsFile.exists()) {
      None
    } else {
      Some(load(detailsFile))
    }
  }

  private def load(file: File): SprintDetails = extract[SprintDetails](parse(Source.fromFile(file).mkString))
}
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

import java.io.{PrintWriter, File}

import org.github.microburn.domain._

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
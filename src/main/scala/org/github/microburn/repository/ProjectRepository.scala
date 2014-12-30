package org.github.microburn.repository

import java.io.File

trait ProjectRepository {
  def sprintRoots: Seq[File]
}

object ProjectRepository {
  def apply(projectRoot: File): ProjectRepository = {
    new ProjectFSRepository(projectRoot)
  }
}

class ProjectFSRepository(projectRoot: File) extends ProjectRepository {
  override def sprintRoots: Seq[File] = {
    Option(projectRoot.listFiles()).toSeq.flatten
  }
}
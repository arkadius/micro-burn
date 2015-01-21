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
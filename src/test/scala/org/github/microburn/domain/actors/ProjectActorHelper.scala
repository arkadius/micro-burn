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
package org.github.microburn.domain.actors

import java.io.File

import net.liftweb.actor.LAFuture
import org.github.microburn.domain.{ProjectConfig, Sprint}
import org.github.microburn.repository.SprintRepository
import org.scalatest.Matchers

import scala.reflect.io.Path

trait ProjectActorHelper extends Matchers {

  import org.github.microburn.util.concurrent.ActorEnrichments._
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import concurrent.duration._
  
  def projectActorWithoutSprints(implicit config: ProjectConfig): ProjectActor = {
    val projectRoot = config.dataRoot
    Path(projectRoot).deleteRecursively()

    new ProjectActor(config, initialFetchToSprintStartAcceptableDelayMinutes = 1.second)
  }
  
  def projectActorWithInitialSprint(sprint: Sprint)
                                   (implicit config: ProjectConfig): ProjectActor = {
    val projectRoot = config.dataRoot
    Path(projectRoot).deleteRecursively()
    SprintRepository(new File(projectRoot, sprint.id), sprint.id).saveSprint(sprint)

    new ProjectActor(config, initialFetchToSprintStartAcceptableDelayMinutes = 1.second)
  }

  def projectHasNoSprint(projectActor: ProjectActor): LAFuture[Unit] = {
    for {
      projectState <- (projectActor ?? GetProjectState).mapTo[ProjectState]
      sprintWithStates = projectState.sprints
    } yield {
      sprintWithStates shouldBe empty
    }
  }

  def projectHasOnlyOneSprint(sprintId: String, projectActor: ProjectActor): LAFuture[Boolean] = {
    for {
      projectState <- (projectActor ?? GetProjectState).mapTo[ProjectState]
      sprintWithStates = projectState.sprints
    } yield {
      sprintWithStates should have length 1
      sprintWithStates.head.id shouldEqual sprintId
      sprintWithStates.head.isActive
    }
  }

  def projectHasOnlyOneActiveSprint(sprintId: String, projectActor: ProjectActor): LAFuture[Unit] = {
    for {
      projectState <- (projectActor ?? GetProjectState).mapTo[ProjectState]
      sprintWithStates = projectState.sprints
    } yield {
      val activeSprints = sprintWithStates.filter(_.isActive)
      activeSprints should have length 1
      activeSprints.head.id shouldEqual sprintId
    }
  }

}
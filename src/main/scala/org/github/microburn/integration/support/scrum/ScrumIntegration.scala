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
package org.github.microburn.integration.support.scrum

import java.util.Date

import net.liftweb.actor.LAFuture
import net.liftweb.common.Box
import org.github.microburn.domain.actors._
import org.github.microburn.domain.{MajorSprintDetails, UserStory}
import org.github.microburn.integration.Integration

class ScrumIntegration(sprintsProvider: SprintsDataProvider, tasksProvider: TasksDataProvider)(projectActor: ProjectActor)
  extends Integration {

  import org.github.microburn.util.concurrent.FutureEnrichments._
  import org.github.microburn.util.concurrent.ActorEnrichments._

  override def updateProject(implicit timestamp: Date): LAFuture[_] = {
    for {
      (currentSprints, updatedSprintIds) <- parallelCurrentAndUpdatedSprints
      _ <- parallelCreateAndUpdate(currentSprints, updatedSprintIds)
    } yield Unit
  }

  private def parallelCurrentAndUpdatedSprints: LAFuture[(ProjectState, Seq[Int])] = {
    val currentStateFuture = (projectActor ?? GetProjectState).mapTo[ProjectState]
      .withLoggingFinished("current sprint ids: " + _.sprints.map(_.id).mkString(", "))
    val updatedIdsFuture = sprintsProvider.allSprintIds.withLoggingFinished("updated sprints ids: " + _.mkString(", "))
    for {
      currentState <- currentStateFuture
      updatedIds <- updatedIdsFuture
    } yield (currentState, updatedIds)
  }

  private def parallelCreateAndUpdate(current: ProjectState, updatedSprintIds: Seq[Int])
                                     (implicit timestamp: Date): LAFuture[_] = {
    val createResultFuture = createNewSprints(current, updatedSprintIds).withLoggingFinished("created sprints: " + _.mkString(", "))
    val updateResultFuture = updateActiveSprints(current).withLoggingFinished("updated sprints: " + _.mkString(", "))
    for {
      _ <- createResultFuture
      _ <- updateResultFuture
    } yield Unit
  }

  private def createNewSprints(current: ProjectState, retrieved: Seq[Int])
                              (implicit timestamp: Date): LAFuture[List[Int]] = {
    val currentIds = current.sprintIds
    val missing = retrieved.filterNot(currentIds.contains)
    val createResults = missing.map { sprintId =>
      for {
        (details, userStories) <- parallelSprintDetailsAndUserStories(sprintId)
        createResult <- (projectActor !< CreateNewSprint(sprintId, details, userStories, timestamp)).mapTo[Box[Int]].map(_.toOption)
      } yield createResult
    }
    LAFuture.collect(createResults : _*).map(_.flatten)
  }

  private def updateActiveSprints(current: ProjectState)
                                 (implicit timestamp: Date): LAFuture[List[Int]] = {
    val updateResults = current.sprints.collect {
      case withDetails if withDetails.isActive =>
        for {
          (details, userStories) <- parallelSprintDetailsAndUserStories(withDetails.id)
          updateResult <- (projectActor ?? UpdateSprint(withDetails.id, userStories, details, timestamp)).mapTo[Int]
        } yield updateResult
    }
    LAFuture.collect(updateResults : _*)
  }

  private def parallelSprintDetailsAndUserStories(sprintId: Int): LAFuture[(MajorSprintDetails, Seq[UserStory])] = {
    val detailsFuture = sprintsProvider.sprintDetails(sprintId).withLoggingFinished(s"sprint details for sprint $sprintId: " + _)
    val tasksFuture = tasksProvider.userStories(sprintId).withLoggingFinished(s"user stories count for sprint $sprintId: " + _.size)
    for {
      details <- detailsFuture
      tasks <- tasksFuture
    } yield (details, tasks)
  }

}
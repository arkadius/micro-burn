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
package org.github.microburn.integration.jira

import dispatch.{Http, as}
import net.liftweb.actor.LAFuture
import org.github.microburn.domain.{TechnicalTask, UserStory}
import org.github.microburn.integration.TasksDataProvider
import org.json4s.JsonAST._

class JiraTasksDataProvider(config: JiraConfig) extends TasksDataProvider {
  import org.github.microburn.util.concurrent.FutureEnrichments._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def userStories(sprintId: String): LAFuture[Seq[UserStory]] = {
    val url = config.jiraUrl / "search" <<? Map(
      "jql" -> s"sprint=$sprintId",
      "fields" -> s"summary,type,status,${config.storyPointsField},parent"
    )
    Http(url OK as.json4s.Json).toLiftFuture.map { jv =>
      val tasks = (jv \ "issues").children.map { issueJson =>
        val JString(taskId) = issueJson \ "key"
        val fields = issueJson \ "fields"
        val parentId = fields \ "parent" \ "key" match {
          case JString(id) => Some(id)
          case _ => None
        }
        val JString(taskName) = fields \ "summary"
        val storyPoints = fields \ config.storyPointsField match {
          case JDecimal(d) => Some(d.toInt)
          case _ => None
        }
        val JString(status) = fields \ "status" \ "id"
        TaskWithParentId(taskId, taskName, storyPoints, parentId, status.toInt)
      }
      val groupedByParent = tasks.groupBy(_.parentId).toMap
      val userStoryTasks = tasks.filter(_.parentId.isEmpty)
      userStoryTasks.map { userStoryTask =>
        val subTasks = groupedByParent.getOrElse(Some(userStoryTask.taskId), Nil)
        createUserStory(userStoryTask, subTasks)
      }
    }
  }

  private def createUserStory(parentTask: TaskWithParentId, subTasks: List[TaskWithParentId]): UserStory = {
    UserStory(parentTask.taskId, parentTask.taskName, parentTask.storyPoints, subTasks.map(createTechnicalTask).toIndexedSeq, parentTask.status)
  }

  private def createTechnicalTask(subTask: TaskWithParentId): TechnicalTask = {
    TechnicalTask(subTask.taskId, subTask.taskName, subTask.storyPoints, subTask.status)
  }

  case class TaskWithParentId(taskId: String, taskName: String, storyPoints: Option[Int], parentId: Option[String], status: Int)
}
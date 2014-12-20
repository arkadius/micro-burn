package org.github.jiraburn.jira

import dispatch.{Http, as}
import net.liftweb.actor.LAFuture
import org.github.jiraburn.domain.{TechnicalTask, UserStory}
import org.json4s.JsonAST._

class TasksDataProvider(config: JiraConfig) {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._
  import scala.concurrent.ExecutionContext.Implicits.global

  def userStories(sprintId: String): LAFuture[Seq[UserStory]] = {
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
    UserStory(parentTask.taskId, parentTask.taskName, parentTask.storyPoints, subTasks.map(createTechnicalTask), parentTask.status)
  }

  private def createTechnicalTask(subTask: TaskWithParentId): TechnicalTask = {
    TechnicalTask(subTask.taskId, subTask.taskName, subTask.storyPoints, subTask.status)
  }

  case class TaskWithParentId(taskId: String, taskName: String, storyPoints: Option[Int], parentId: Option[String], status: Int)
}

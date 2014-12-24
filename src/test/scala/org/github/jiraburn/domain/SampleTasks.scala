package org.github.jiraburn.domain

object SampleTasks {

  private var currentId = 0

  private def nextId = {
    currentId += 1
    currentId.toString
  }

  def openedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil)(implicit config: ProjectConfig) = {
    val id = nextId
    UserStory(id, s"User Story $id", Some(sp), technical.toList, ProjectConfigUtils.firstNotClosingStatus)
  }

  def closedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil)(implicit config: ProjectConfig) = {
    val id = nextId
    UserStory(id, s"User Story $id", Some(sp), technical.toList, ProjectConfigUtils.firstClosingStatus)
  }

  def openedTechnicalTask(optionalSP: Option[Int])(implicit config: ProjectConfig) = {
    val id = nextId
    TechnicalTask(id, s"Technical Task $id", optionalSP, ProjectConfigUtils.firstNotClosingStatus)
  }

  def closedTechnicalTask(optionalSP: Option[Int])(implicit config: ProjectConfig) = {
    val id = nextId
    TechnicalTask(id, s"Technical Task $id", optionalSP, ProjectConfigUtils.firstClosingStatus)
  }

}
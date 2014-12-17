package org.github.jiraburn.domain

object TaskGenerator {

  private var currentId = 0

  private def nextId = {
    currentId += 1
    currentId.toString
  }

  def openedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil)(implicit config: ProjectConfig) = {
    val id = nextId
    UserStory(id, s"User Story $id", Some(sp), technical.toList, config.firstNotClosingStatus)
  }

  def completedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil)(implicit config: ProjectConfig) = {
    val id = nextId
    UserStory(id, s"User Story $id", Some(sp), technical.toList, config.firstClosingStatus)
  }

  def openedTechnicalTask(optionalSP: Option[Int])(implicit config: ProjectConfig) = {
    val id = nextId
    TechnicalTask(id, s"Technical Task $id", optionalSP, config.firstNotClosingStatus)
  }

  def completedTechnicalTask(optionalSP: Option[Int])(implicit config: ProjectConfig) = {
    val id = nextId
    TechnicalTask(id, s"Technical Task $id", optionalSP, config.firstClosingStatus)
  }

}
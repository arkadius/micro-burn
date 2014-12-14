package com.example.domain

object TaskGenerator {

  private var currentId = 0

  private def nextId = {
    currentId += 1
    currentId.toString
  }

  def openedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil) = {
    val id = nextId
    UserStory(id, s"User Story $id", Some(sp), technical.toList, Opened)
  }

  def completedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil) = {
    val id = nextId
    UserStory(id, s"User Story $id", Some(sp), technical.toList, Completed)
  }

  def openedTechnicalTask(optionalSP: Option[Int]) = {
    val id = nextId
    TechnicalTask(id, s"Technical Task $id", optionalSP, Opened)
  }

  def completedTechnicalTask(optionalSP: Option[Int]) = {
    val id = nextId
    TechnicalTask(id, s"Technical Task $id", optionalSP, Completed)
  }

}
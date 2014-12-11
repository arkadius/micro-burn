package com.example.domain

object TaskGenerator {

  private var currentId = 0

  private def nextId = {
    currentId += 1
    currentId.toString
  }

  def openedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil) =
    UserStory(nextId, Some(sp), technical.toList, Opened)

  def completedUserStory(sp: Int, technical: Seq[TechnicalTask] = Nil) =
    UserStory(nextId, Some(sp), technical.toList, Completed)


  def openedTechnicalTask(optionalSP: Option[Int]) = TechnicalTask(nextId, optionalSP, Opened)

  def completedTechnicalTask(optionalSP: Option[Int]) = TechnicalTask(nextId, optionalSP, Completed)

}

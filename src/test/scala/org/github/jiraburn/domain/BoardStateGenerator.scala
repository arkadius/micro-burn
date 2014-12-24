package org.github.jiraburn.domain

import java.util.Date

import org.scalacheck.Gen

object BoardStateGenerator {

  def generator(date: Date): Gen[BoardState] =
    for {
      userStories <- Gen.listOf(TasksGenerator.userStoryGenerator)
    } yield BoardState(userStories, date)
  
}

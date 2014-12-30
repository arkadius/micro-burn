package org.github.microburn.domain.generator

import java.util.Date

import org.github.microburn.domain.BoardState
import org.scalacheck.Gen

object BoardStateGenerator {

  def generator(date: Date): Gen[BoardState] =
    for {
      userStories <- Gen.listOf(TasksGenerator.userStoryGenerator)
    } yield BoardState(userStories, date)
  
}

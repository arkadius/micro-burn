package org.github.micoburn.domain.generator

import java.util.Date

import org.github.micoburn.domain.BoardState
import org.scalacheck.Gen

object BoardStateGenerator {

  def generator(date: Date): Gen[BoardState] =
    for {
      userStories <- Gen.listOf(TasksGenerator.userStoryGenerator)
    } yield BoardState(userStories, date)
  
}

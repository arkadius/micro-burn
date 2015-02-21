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
package org.github.microburn.domain

import java.util.Date

import org.github.microburn.domain.generator.{BoardChangesGenerator, BoardStateGenerator}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class BoardStateRandomTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers {

  implicit val config = ProjectConfigUtils.defaultConfig
  implicit val testConfig = PropertyCheckConfig(minSuccessful = 10)

  it should "return empty result for the same boards" in {
    forAll(BoardStateGenerator.generator(new Date(0))) { generated =>
      generated.diff(generated) shouldBe empty
    }
  }
  
  it should "do round-trip" in {
    forAll(initialBoardAndChangesGenerator) {
      case InitialBoardAndChanges(initialBoard, changes) =>
        val boardsAndEvents = changes.scanLeft(initialBoard, Seq.empty[TaskEvent]) {
          case ((prevBoard, events), changedBoard) =>
            val newAddedEvents = prevBoard.diff(changedBoard)
            (changedBoard, newAddedEvents)
        }

        boardsAndEvents.drop(1).foldLeft(initialBoard) {
          case (prevBoard, (boardAfterGeneratedChanges, events)) =>
            val boardAfterEventsAccumulation = events.foldLeft(prevBoard) { (board, event) =>
              board.plus(event)
            }
            withMoreReadeableClue(prevBoard, events, boardAfterGeneratedChanges, boardAfterEventsAccumulation) { // mocno zwalnia testy
              boardAfterEventsAccumulation shouldEqual boardAfterGeneratedChanges
            }
            boardAfterEventsAccumulation
        }
    }
  }


  private def withMoreReadeableClue(prevBoard: BoardState,
                                    events: Seq[TaskEvent],
                                    boardAfterGeneratedChanges: BoardState,
                                    boardAfterEventsAccumulation: BoardState)(f: => Unit) = {
    withClue(s"""
                |***** BEGIN *****
                |$prevBoard
                | ++
                |${events.mkString(",\n")}
                |
                | SHOULD EQUAL
                |
                |$boardAfterGeneratedChanges
                |
                | BUT IS
                |
                |$boardAfterEventsAccumulation
                |***** END ******
                |""".stripMargin) {
      deepCheck(boardAfterEventsAccumulation, boardAfterGeneratedChanges)
      deepCheck(boardAfterGeneratedChanges, boardAfterEventsAccumulation)
      f
    }
  }

  private def deepCheck(boardAfterEventsAccumulation: BoardState, boardAfterGeneratedChanges: BoardState) {
    boardAfterEventsAccumulation.date shouldEqual boardAfterGeneratedChanges.date
    boardAfterEventsAccumulation.userStories.foreach { one =>
      val twoFiltered = boardAfterGeneratedChanges.userStories.filter(_.taskId == one.taskId)
      twoFiltered should have length 1
      val two = twoFiltered.head
      one.technicalTasksWithoutParentId.foreach { oneTech =>
        val twoTechFiltered = two.technicalTasksWithoutParentId.filter(_.taskId == oneTech.taskId)
        twoTechFiltered should have length 1
        val twoTech = twoTechFiltered.head
        oneTech.taskName shouldEqual twoTech.taskName
        oneTech.optionalStoryPoints shouldEqual twoTech.optionalStoryPoints
        oneTech.status shouldEqual twoTech.status
        oneTech shouldEqual twoTech
      }
      one.taskName shouldEqual two.taskName
      one.optionalStoryPoints shouldEqual two.optionalStoryPoints
      one.status shouldEqual two.status
      one shouldEqual two
    }
  }

  private val initialBoardAndChangesGenerator: Gen[InitialBoardAndChanges] =
    for {
      initialBoardState <- BoardStateGenerator.generator(new Date(0))
      nChanges <- Gen.chooseNum(1, 10)
      changes = {
        // rozwiązanie leniwe ze Gen.sequence(streamOfGen) powodowało, że dostawaliśmy zmiany w stosunku do innych niż w wyniku tablic
        lazy val boardStatesStream = Stream.iterate[Option[BoardState]](Some(initialBoardState)) { optionalPrevBoard =>
          optionalPrevBoard.flatMap { prevBoard =>
            BoardChangesGenerator.changesGenerator(prevBoard).sample
          }
        }
        boardStatesStream.drop(1).take(nChanges).takeWhile(_.isDefined).map(_.get)
      }
      _ = {
        println("Generated changes count: " + changes.size)
      }
    } yield InitialBoardAndChanges(initialBoardState, changes)

  case class InitialBoardAndChanges(initial: BoardState, changes: Seq[BoardState]) {
    override def toString: String = initial.toString
  }
}
package org.github.jiraburn.domain

import java.util.Date

import org.scalacheck.Gen
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BoardStateRandomTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers {

  import collection.convert.wrapAsScala._

  /*it should "retun empty result for the same boards" in {
    forAll(BoardStateGenerator.generator(new Date(0))) { generated =>
      generated.diff(generated) shouldBe empty
    }
  }

  it should "generate removed event" in {
    forAll(
      for {
        generated <- BoardStateGenerator.generator(new Date(0))
        afterRemoveChange <- BoardChangesGenerator.removeUserStoryGenerator(generated)
      } yield (generated, afterRemoveChange)
    ) {
      case (generated, afterRemoveChange) =>
        generated.diff(afterRemoveChange).flatMap {
          case r:TaskRemoved => None
          case other => Some(other)
        } shouldBe empty
    }
  }*/
  
  it should "do round-trip" in {
    forAll(initialBoardAndChangesGenerator) {
      case InitialBoardAndChanges(initialBoard, changes) =>
        ((initialBoard +: changes) zip changes).foreach {
          case (prev, next) =>
            val prevIds = prev.userStories.map(_.taskId)
            val nextIds = next.userStories.map(_.taskId)
            withClue(s"$nextIds -- $prevIds : ") {
              (nextIds -- prevIds) shouldBe empty
            }
        }

        lazy val boardAndEventsStream: Stream[(BoardState, Seq[TaskEvent])] =
          (initialBoard, Nil) #::
            (boardAndEventsStream zip changes).map {
              case ((prevBoard, events), changedBoard) =>
                val newAddedEvents = prevBoard.diff(changedBoard)
                withClue(
                  s"""
                     |***** BEGIN *****
                     |$prevBoard
                     |
                     | =?=
                     |
                     |$changedBoard
                     |
                     | SHOULD NOT PRODUCE ADDED EVENTS BUT PRODUCE:
                     |
                     |${newAddedEvents.mkString(",\n")}
                     |***** END ******
                     |""".stripMargin
                ) {
                  newAddedEvents.flatMap {
                    case r: TaskRemoved => None
                    case other => Some(other)
                  } shouldBe empty
                }
                (changedBoard, newAddedEvents)
            }
        boardAndEventsStream.drop(1).foldLeft(initialBoard) {
          case (prevBoard, (boardAfterGeneratedChanges, events)) =>
            val boardAfterEventsAccumulation =
              try {
                events.foldLeft(prevBoard) { (board, event) =>
                  board.plus(event)
                }
              } catch {
                case ex: Exception =>
                  System.err.println(
                    s"""***** BEGIN *****
                       |$changes
                       |""".stripMargin
                  )
                  throw ex
              }

            withClue(
              s"""
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
               |""".stripMargin
            ) {
              boardAfterEventsAccumulation.userStories.foreach { one =>
                val two = boardAfterGeneratedChanges.userStories.find(_.taskId == one.taskId).get
                one.technicalTasksWithoutParentId.foreach { oneTech =>
                  val twoTech = two.technicalTasksWithoutParentId.find(_.taskId == oneTech.taskId).get
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
              boardAfterEventsAccumulation shouldEqual boardAfterGeneratedChanges
            }
            boardAfterEventsAccumulation
        }
    }

  }

  val initialBoardAndChangesGenerator: Gen[InitialBoardAndChanges] =
    for {
      initialBoardState <- BoardStateGenerator.generator(new Date(0))
      nChanges <- Gen.posNum[Int]
      // TODO: changes nie są tylko usuwające
      changes <- {
        lazy val boardGeneratorsStream: Stream[Gen[BoardState]] = Stream.iterate(Gen.const(initialBoardState)) { prevGen =>
          for {
            prevBoard <- prevGen
            newBoard <- BoardChangesGenerator.changesGenerator(prevBoard)
          } yield newBoard
        }
        Gen.sequence(boardGeneratorsStream.take(nChanges)).map(_.drop(1))
      }
    } yield InitialBoardAndChanges(initialBoardState, changes)

  case class InitialBoardAndChanges(initial: BoardState, changes: Seq[BoardState]) {
    override def toString: String = initial.toString
  }

  case class StateWithIndex(state: BoardState, index: Int)

}
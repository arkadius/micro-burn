package org.github.jiraburn.domain

import java.util.Date

import org.scalacheck.Gen
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class SprintRandomTest extends FlatSpec with GeneratorDrivenPropertyChecks with Matchers {

  import collection.convert.wrapAsScala._

  it should "do round-trip" in {
    forAll(initialSprintAndChangesGenerator) {
      case InitialSprintAndChanges(initialSprint, changes) =>
        lazy val sprintAndEventsStream: Stream[(Sprint, Seq[TaskEvent])] =
          (initialSprint, Nil) #::
            (sprintAndEventsStream zip changes).map {
              case ((prevSprint, events), boardStateWithIndex) =>
                val updateResult = prevSprint.update(boardStateWithIndex.state.userStories, finishSprint = false)(new Date(boardStateWithIndex.index))
                (updateResult.updatedSprint, updateResult.newAddedEvents)
            }
        sprintAndEventsStream.foldLeft(initialSprint.currentBoard) {
          case (board, (sprintAfterAllChanges, events)) =>
            val boardAfterEventsAccumulation = events.foldLeft(board) { (board, event) =>
              board.plus(event)
            }

            withClue(
              s"""
               |***** BEGIN *****
               |$board
               | ++
               |${events.mkString(",\n")}
               |
               | SHOULD EQUAL
               |
               |${sprintAfterAllChanges.currentBoard}
               |
               | BUT IS
               |
               |$boardAfterEventsAccumulation
               |***** END ******
               |""".stripMargin
            ) {
              boardAfterEventsAccumulation.date shouldEqual sprintAfterAllChanges.currentBoard.date
//              boardAfterEventsAccumulation.userStories.foreach { one =>
//                val two = sprintAfterAllChanges.currentBoard.userStories.find(_.taskId == one.taskId).get
//                one.technicalTasksWithoutParentId.foreach { oneTech =>
//                  val twoTech = two.technicalTasksWithoutParentId.find(_.taskId == oneTech.taskId).get
//                  oneTech.taskName shouldEqual twoTech.taskName
//                  oneTech.optionalStoryPoints shouldEqual twoTech.optionalStoryPoints
//                  oneTech.status shouldEqual twoTech.status
//                  oneTech shouldEqual twoTech
//                }
//                one.taskName shouldEqual two.taskName
//                one.optionalStoryPoints shouldEqual two.optionalStoryPoints
//                one.status shouldEqual two.status
//                one shouldEqual two
//              }
              boardAfterEventsAccumulation shouldEqual sprintAfterAllChanges.currentBoard
            }
            boardAfterEventsAccumulation
        }
    }

  }

  val initialSprintAndChangesGenerator: Gen[InitialSprintAndChanges] =
    for {
      initialSprint <- SprintGenerator.withEmptyEvents
      nChanges <- Gen.posNum[Int]
      changes <- {
        lazy val boardGeneratorsStream: Stream[Gen[BoardState]] = Stream.iterate(Gen.const(initialSprint.currentBoard)) { prevGen =>
          for {
            prevBoard <- prevGen
            newBoard <- BoardChangesGenerator.changesGenerator(prevBoard)
          } yield newBoard
        }
        Gen.sequence(boardGeneratorsStream.drop(1).take(nChanges)).map(_.zipWithIndex.map(StateWithIndex.apply _ tupled))
      }
    } yield InitialSprintAndChanges(initialSprint, changes)


  case class InitialSprintAndChanges(initial: Sprint, changes: Seq[StateWithIndex]) {
    override def toString: String = initial.currentBoard.toString
  }

  case class StateWithIndex(state: BoardState, index: Int)

}

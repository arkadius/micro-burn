package org.github.microburn.domain

import java.util.Date

case class Sprint(id: String,
                  details: SprintDetails,
                  private val initialBoard: BoardState,
                  currentBoard: BoardState,
                  private val events: Seq[TaskEvent]) {

  def isActive = details.isActive

  def initialDate: Date = initialBoard.date

  def initialStoryPointsSum: Int = initialBoard.userStoriesStoryPointsSum

  def update(updatedUserStories: Seq[UserStory], finishSprint: Boolean)
            (timestamp: Date): SprintUpdateResult = {
    val updatedBoard = BoardState(updatedUserStories, timestamp)
    val newAddedEvents = currentBoard.diff(updatedBoard)
    val finished = isActive && finishSprint
    
    val updatedSprint = copy(
      details = if (finished) details.finish else details,
      currentBoard = updatedBoard,
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(updatedBoard, updatedSprint, newAddedEvents, finished, timestamp)
  }
  
  def columnStatesHistory(implicit config: ProjectConfig): Seq[DateWithColumnsState] = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    lazy val boardStateStream: Stream[BoardState] =
      initialBoard #::
        (boardStateStream zip eventsSortedAndGrouped).map {
          case (prevBoard, currEventsGroup) =>
            currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
              boardAcc.plus(event)
            }
        }

    boardStateStream.map(_.columnsState).toSeq
  }
}

case class SprintDetails(name: String, start: Date, end: Date, isActive: Boolean) {
  def finished = !isActive

  def finish = copy(isActive = false)
}

object SprintDetails {
  def apply(name: String, start: Date, end: Date): SprintDetails = SprintDetails(name, start, end, isActive = true)
}

case class SprintUpdateResult(private val stateBeforeUpdate: BoardState, updatedSprint: Sprint, newAddedEvents: Seq[TaskEvent], sprintFinished: Boolean, timestamp: Date) {
  def importantChange: Boolean =  importantDetailsChange || importantBoardChange

  def importantDetailsChange: Boolean = sprintFinished // co ze zmianą nazwy/dat?
  
  def importantBoardChange: Boolean = newAddedEvents.nonEmpty
}

object Sprint {
  def withEmptyEvents(id: String, details: SprintDetails, state: BoardState): Sprint =
    Sprint(id, details, initialBoard = state, currentBoard = state, IndexedSeq.empty)
}
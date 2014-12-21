package org.github.jiraburn.domain

import java.util.Date

case class Sprint(id: String,
                  details: SprintDetails,
                  private val initialState: SprintState,
                  currentState: SprintState,
                  private val events: Seq[TaskChanged]) {

  def isActive = details.isActive

  def initialDate: Date = initialState.date

  def initialStoryPointsSum: Int = {
    initialState.userStories.flatMap { userStory =>
      userStory.optionalStoryPoints
    }.sum
  }

  def update(updatedUserStories: Seq[UserStory], finishSprint: Boolean)
            (timestamp: Date)
            (implicit config: ProjectConfig): SprintUpdateResult = {
    val updatedState = SprintState(updatedUserStories, timestamp)
    val currentBoard = BoardState(currentState)
    val updatedBoard = BoardState(updatedState)
    val newAddedEvents = currentBoard.diff(updatedBoard)
    val finished = isActive && finishSprint
    
    val updatedSprint = copy(
      details = if (finished) details.finish else details,
      currentState = updatedState,
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(currentState, updatedSprint, newAddedEvents, finished, timestamp)
  }
  
  def columnStatesHistory(implicit config: ProjectConfig): Seq[DateWithColumnsState] = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    lazy val boardStateStream: Stream[BoardState] =
      BoardState(initialState) #::
        (boardStateStream zip eventsSortedAndGrouped).map {
          case (prevBoard, currEventsGroup) =>
            currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
              boardAcc.plus(event)
            }
        }

    boardStateStream.map(_.columnsState).toSeq
  }
}

case class SprintState(userStories: Seq[UserStory], date: Date)

case class SprintDetails(name: String, start: Date, end: Date, isActive: Boolean) {
  def finished = !isActive

  def finish = copy(isActive = false)
}

object SprintDetails {
  def apply(name: String, start: Date, end: Date): SprintDetails = SprintDetails(name, start, end, isActive = true)
}

case class SprintUpdateResult(private val stateBeforeUpdate: SprintState, updatedSprint: Sprint, newAddedEvents: Seq[TaskChanged], sprintFinished: Boolean, timestamp: Date) {
  def importantChange: Boolean =  importantDetailsChange || importantEventsChange

  def importantDetailsChange: Boolean = sprintFinished // co ze zmianą nazwy/dat?
  
  def importantEventsChange: Boolean = newAddedEvents.nonEmpty
}

object Sprint {
  def withEmptyEvents(id: String, details: SprintDetails, state: SprintState): Sprint =
    Sprint(id, details, initialState = state, currentState = state, Nil) 
}
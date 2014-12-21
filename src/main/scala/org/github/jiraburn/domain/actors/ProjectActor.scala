package org.github.jiraburn.domain.actors

import java.io.File
import java.util.Date

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.{LAFuture, LiftActor}
import net.liftweb.common.{Failure, Full, Box}
import org.github.jiraburn.domain.{DateWithStoryPoints, ProjectConfig, SprintDetails, UserStory}
import org.github.jiraburn.repository.ProjectRepository

import scalaz.Scalaz._

class ProjectActor(projectRoot: File, config: ProjectConfig, sprintChangeNotifyingActor: LiftActor) extends LiftActor {
  import org.github.jiraburn.util.concurrent.FutureEnrichments._

  private val sprintFactory = new SprintActorFactory(projectRoot, config, sprintChangeNotifyingActor)
  private val projectRepo = ProjectRepository(projectRoot)

  private var sprintActors: Map[String, SprintActor] = (
    for {
      sprintRoot <- projectRepo.sprintRoots
      sprintId = sprintRoot.getName
      sprintActor <- sprintFactory.fromRepo(sprintId)
    } yield (sprintId, sprintActor)
  ).toMap

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case GetSprintsWithStates =>
      val sprintWithStateFutures = sprintActors.map {
        case (sprintId, sprintActor) =>
          (sprintActor !< IsActive).mapTo[Boolean] map { isActive =>
            SprintWithState(sprintId, isActive)
          }
      }.toSeq
      reply(collectWithWellEmptyListHandling(sprintWithStateFutures))
    case CreateNewSprint(sprintId, details, userStories, timestamp) =>
      sprintActors += sprintId -> sprintFactory.createSprint(sprintId, details, userStories, timestamp)
      reply(sprintId)
    case update: UpdateSprint =>
      reply(sprintActors(update.sprintId) !< update)
    case getHistory: GetStoryPointsHistory =>
      val future = sprintActors.get(getHistory.sprintId).map { sprintActor =>
        (sprintActor !< getHistory).map(Full(_))
      }.getOrElse(LAFuture[Box[_]](() => Failure("Sprint with given id does not exist")))
      reply(future)
    case Close =>
      val sprintClosedFutures = sprintActors.values.map { sprintActor =>
        (sprintActor !< Close).mapTo[Unit]
      }.toSeq
      reply(collectWithWellEmptyListHandling(sprintClosedFutures))
  }
}

case object GetSprintsWithStates

case class SprintWithState(sprintId: String, isActive: Boolean)

case class CreateNewSprint(sprintId: String, details: SprintDetails, userStories: Seq[UserStory], timestamp: Date)

case class UpdateSprint(sprintId: String, userStories: Seq[UserStory], finishSprint: Boolean, timestamp: Date)

case class GetStoryPointsHistory(sprintId: String)

case class SprintHistory(initialStoryPoints: Int,
                         sprintDetails: SprintDetails,
                         private val changes: Seq[DateWithStoryPoints])
                        (implicit config: ProjectConfig) {
  
  def columnsHistoryFor(now: Date): List[ColumnWithStoryPointsHistory] = {
    val firstRealChangeNotBeforeSprintStart = !changes.headOption.exists(!_.date.after(sprintDetails.start))
    val toPrepend = firstRealChangeNotBeforeSprintStart.option(DateWithStoryPoints.zero(sprintDetails.start))

    val nowOrSprintsEndForFinished = sprintDetails.finished.option(sprintDetails.end).getOrElse(now)
    val lastRealChangeNotAfterNow = !changes.lastOption.exists(!_.date.before(nowOrSprintsEndForFinished))
    val toAppend = lastRealChangeNotAfterNow.option {
      changes.lastOption.map { last =>
        last.copy(date = nowOrSprintsEndForFinished)
      } getOrElse DateWithStoryPoints.zero(nowOrSprintsEndForFinished)
    }

    val fullHistory = toPrepend ++ changes ++ toAppend
    val withBaseAdded = fullHistory.map(_.plus(initialStoryPoints))
    config.boardColumns.map { column =>
      val storyPointsForColumn = withBaseAdded.map { allColumnsInfo =>
        val storyPoints = allColumnsInfo.storyPointsForColumn(column.index)
        DateWithStoryPointsForSingleColumn(allColumnsInfo.date, storyPoints)
      }.toList
      ColumnWithStoryPointsHistory(column.name, storyPointsForColumn)
    }
  }
}

case class ColumnWithStoryPointsHistory(name: String, storyPointsChanges: List[DateWithStoryPointsForSingleColumn])

case class DateWithStoryPointsForSingleColumn(date: Date, storyPoints: Int)

case object Close

object ProjectActor {
  def apply(sprintChangeNotifyingActor: LiftActor): ProjectActor = {
    val config = ConfigFactory.load()
    val projectRoot = new File(config.getString("data.project.root"))
    val projectConfig = ProjectConfig(config)
    new ProjectActor(projectRoot, projectConfig, sprintChangeNotifyingActor)
  }
}
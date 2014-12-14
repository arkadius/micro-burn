package com.example.actors

import java.io.File

import com.example.repository.ProjectRepository
import com.typesafe.config.ConfigFactory
import net.liftweb.actor.LiftActor

class ProjectActor(projectRoot: File, sprintChangeNotifyingActor: LiftActor) extends LiftActor {
  private val sprintFactory = new SprintActorFactory(projectRoot, sprintChangeNotifyingActor)
  private val projectRepo = ProjectRepository(projectRoot)

  private var sprintActors: Map[String, SprintActor] = (
    for {
      sprintRoot <- projectRepo.sprintRoots
      sprintId = sprintRoot.getName
      sprintActor <- sprintFactory.fromRepo(sprintId)
    } yield (sprintId, sprintActor)
  ).toMap

  override protected def messageHandler: PartialFunction[Any, Unit] = {
    case GetActiveSprints =>
      val active = sprintActors.map {
        case (sprintId, sprintActor) =>
          // TODO
      }
    // TODO update, itp
  }
}

case object GetActiveSprints

object ProjectActor {
  def apply(sprintChangeNotifyingActor: LiftActor): ProjectActor = {
    val config = ConfigFactory.load()
    val projectRoot = new File(config.getString("data.project.root"))
    new ProjectActor(projectRoot, sprintChangeNotifyingActor)
  }
}
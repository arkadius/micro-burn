package org.github.jiraburn.actors

import net.liftweb.actor.{LAFuture, LiftActor}

object LiftActorEnrichments {
  import org.github.jiraburn.actors.FutureEnrichments._

  implicit class EnrichedLiftActor(actor: LiftActor) {    
    def ??(msg: Any): LAFuture[Any] = {
      val futureOfFuture = (actor !< msg).mapTo[LAFuture[Any]]
      for {
        future <- futureOfFuture
        result <- future
      } yield result
    }
  }
}

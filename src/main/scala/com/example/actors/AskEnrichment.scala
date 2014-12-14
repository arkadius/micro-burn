package com.example.actors

import net.liftweb.actor.LiftActor

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration.FiniteDuration

trait AskEnrichment { self: LiftActor =>

  def ?? (msg: Any, timeout: FiniteDuration): Future[Any] = {
    val box = self !! (msg, timeout.toMillis)
    box.asA[Future[Any]].openOr(throw new TimeoutException(s"After $timeout timeout occurred"))
  }


}

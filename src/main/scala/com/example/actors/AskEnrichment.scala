package com.example.actors

import net.liftweb.actor.{LAFuture, LiftActor}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

trait AskEnrichment { self: LiftActor =>

  def ?? (msg: Any, timeout: FiniteDuration): LAFuture[Any] = {
    val box = self !! (msg, timeout.toMillis)
    box.asA[LAFuture[Any]].openOr(throw new TimeoutException(s"After $timeout timeout occurred"))
  }

}

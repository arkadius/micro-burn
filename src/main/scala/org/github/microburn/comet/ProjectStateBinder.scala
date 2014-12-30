package org.github.microburn.comet

import net.liftmodules.ng.{BindingToClient, SimpleNgModelBinder}
import net.liftweb.actor.LAFuture
import net.liftweb.http.CometListener
import org.github.microburn.ApplicationContext
import org.github.microburn.domain.actors.ProjectState

class ProjectStateBinder
  extends SimpleNgModelBinder("projectState", ProjectState(Nil))
  with BindingToClient
  with CometListener {

  override protected def registerWith = ApplicationContext().projectActor

  override def lowPriority: PartialFunction[Any, Unit] = resendState orElse super.lowPriority

  private def resendState: PartialFunction[Any, Unit] = {
    case f:LAFuture[_] =>
      val projectStateFuture = f.asInstanceOf[LAFuture[ProjectState]]
      projectStateFuture.onSuccess(this ! _)
  }
}
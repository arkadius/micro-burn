/*
 * Copyright 2015 the original author or authors.
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn.comet

import net.liftmodules.ng.{BindingToClient, SimpleNgModelBinder}
import net.liftweb.actor.LAFuture
import net.liftweb.http.CometListener
import org.github.microburn.ApplicationContext
import org.github.microburn.domain.actors.{BoardStateChanged, ProjectState}

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
    case BoardStateChanged(sprintId) =>
      scope.emit("boardStateChanged", sprintId)
  }
}
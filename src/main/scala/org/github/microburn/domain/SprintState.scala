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
package org.github.microburn.domain

sealed trait SprintState {
  def canMoveTo(newState: SprintState): Boolean = newState == this || canMoveToOther(newState)
  protected def canMoveToOther(newState: SprintState): Boolean
}

case object ActiveState extends SprintState {
  override protected def canMoveToOther(newState: SprintState): Boolean = newState == FinishedState
}

case object FinishedState extends SprintState {
  override protected def canMoveToOther(newState: SprintState): Boolean = newState == RemovedState
}

case object RemovedState extends SprintState {
  override protected def canMoveToOther(newState: SprintState): Boolean = false
}

object SprintState {
  def apply(isActive: Boolean) =
    if (isActive)
      ActiveState
    else
      FinishedState
}
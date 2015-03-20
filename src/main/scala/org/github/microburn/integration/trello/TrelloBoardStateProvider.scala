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
package org.github.microburn.integration.trello

import net.liftweb.actor.LAFuture
import org.github.microburn.domain._
import org.github.microburn.integration.support.kanban.BoardStateProvider

class TrelloBoardStateProvider(config: TrelloConfig) extends BoardStateProvider {
  private val cardsProvider = new TrelloCardsProvider(config)

  override def currentUserStories: LAFuture[Seq[UserStory]] = cardsProvider.cards.map { cards =>
    cards.map(toUserStory)
  }

  def toUserStory(card: Card): UserStory = {
    val technicalTasks = card.checklistItems.map { item =>
      TechnicalTask(item.id, item.extractedName, item.optionalSp, statusForTaskInColumn(card.columnId, item))
    }.toIndexedSeq
    UserStory(card.id, card.extractedName, card.optionalSp, technicalTasks, SpecifiedStatus(card.columnId))
  }

  private def statusForTaskInColumn(columnId: String, task: ChecklistItem) = {
    if (task.closed) {
      TaskCompletedStatus
    } else {
      SpecifiedStatus(columnId)
    }
  }
}
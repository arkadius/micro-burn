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
import org.github.microburn.domain.{TechnicalTask, UserStory}
import org.github.microburn.integration.support.kanban.BoardStateProvider

import scala.math.BigDecimal.RoundingMode
import scalaz._
import Scalaz._

class TrelloBoardStateProvider(config: TrelloConfig) extends BoardStateProvider {
  private final val SP_SPLITTED_BETWEEN_TECHICAL_SCALE: Int = 1

  private val cardsProvider = new TrelloCardsProvider(config)

  override def currentUserStories: LAFuture[Seq[UserStory]] = cardsProvider.cards.map { cards =>
    cards
      // TODO: powinniśmy też uwzględniać zamknięte - gdy ktoś zarchiwizuje zadanie przed zakończeniem sprintu
      // (będzie przydatne zwłaszcza po wprowadzeniu automatycznego rozpoczycznania/zakańczania sprintów).
      // Należy przy tym pamiętać o problemach:
      // - ktoś może zamknąć zadanie, bo przypadkowo utworzył => możemy brać pod uwagę zamknięte tylko w kolumnie DONE
      // - w jaki sposób zweryfikować, że zadanie zostało zamknięte w tym sprincie, a nie w poprzednich?
      // + możemy patrzeć na dateLastActivity - mogą być rozjechane zegary, a ktoś może zamknąć zadania chwilę po/przed
      // zakończeniem sprintu
      // + możemy odrzucać zadania zamknięte w poprzednich sprintach - mogło być ponownie otwarte a potem jeszcze raz zamknięte?
      // - czy musimy się martwić, gdy automatczynie zakończy się sprint, a zadania nie będę przesunięte? => raczej nie
      .filterNot(_.closed)
      .map(toUserStory)
  }

  def toUserStory(card: Card): UserStory = {
    val definedIfSignificantPointsPerTechnical = computePointsPerTechnical(card)
    val technicalTasks = card.checkListItems.map { item =>
      TechnicalTask(item.id, item.name, item.optionalSp orElse definedIfSignificantPointsPerTechnical, card.columnId) // TODO: uwzględniać closed
    }.toIndexedSeq
    UserStory(card.id, card.name, card.optionalSp, technicalTasks, card.columnId)
  }

  private def computePointsPerTechnical(card: Card): Option[BigDecimal] = {
    val definedTechnicalPoints = card.checkListItems.flatMap(_.optionalSp).sum
    val sumPointsToSplitBetweenTechnical = (card.optionalSp.getOrElse(BigDecimal(0)) - definedTechnicalPoints).max(0)
    val technicalWithoutDefinedSp = card.checkListItems.count(_.optionalSp.isEmpty)
    Option(technicalWithoutDefinedSp)
      .filter(_ > 0)
      .map { technicalCountGt0 => (sumPointsToSplitBetweenTechnical / technicalCountGt0).setScale(SP_SPLITTED_BETWEEN_TECHICAL_SCALE, RoundingMode.FLOOR) }
      .filter(_ > 0)
  }
}
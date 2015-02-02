package org.github.microburn.integration.trello

import org.github.microburn.integration.support.kanban.StoryPointsFromName

trait TrelloTask {
  def closed: Boolean

  protected val name: String

  val StoryPointsFromName(optionalSp, extractedName) = name
}

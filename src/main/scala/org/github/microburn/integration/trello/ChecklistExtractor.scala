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

import org.github.microburn.integration.support.kanban.StoryPointsFromName
import org.json4s.JsonAST._

object ChecklistExtractor {

  def extract(jv: JValue): Seq[ChecklistItem] = {
    val items = jv.foldField(IndexedSeq.empty[JValue]) {
      case (acc, JField("checkItems", JArray(arr))) => acc ++ arr
      case (acc, _) => acc
    }
    items.map { item =>
      val JString(id) = item \ "id"
      val JString(name) = item \ "name"
      val JString(state) = item \ "state"
      val completedStatus = state == "complete"
      ChecklistItem(id, name, completedStatus)
    }
  }

}

case class ChecklistItem(id: String, name: String, closed: Boolean) extends TrelloTask
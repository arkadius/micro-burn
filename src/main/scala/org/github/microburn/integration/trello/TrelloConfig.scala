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

import com.typesafe.config.Config
import dispatch._

class TrelloConfig(trelloApiUrl: Req,
                   appKey: String,
                   token: String,
                   val boardId: String) {

  def prepareUrl(f: Req => Req) = {
    f(trelloApiUrl) <:< headers <<? authorizationParams
  }
  
  private def headers = Map(
    "Accept-Charset" -> "utf-8"
  )

  private def authorizationParams = Map(
    "key" -> appKey,
    "token" -> token
  )

}

object TrelloConfig {
  def apply(config: Config): TrelloConfig = {
    TrelloConfig(
      config.getString("url"),
      config.getString("appKey"),
      config.getString("token"),
      config.getString("boardId")
    )
  }

  def apply(trelloApiUrl: String,
            appKey: String,
            token: String,
            boardId: String): TrelloConfig = {
    new TrelloConfig(
      trelloApiUrl = url(trelloApiUrl),
      appKey = appKey,
      token = token,
      boardId = boardId
    )
  }
}
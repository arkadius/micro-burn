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

import java.util.Date

object SampleSprint {

  val sampleDetails = SprintDetails(
    name = "fooName",
    start = new Date(1000),
    end = new Date(2000)
  )

  def withEmptyEvents(userStories: UserStory*): Sprint = {
    Sprint.withEmptyEvents("foo", sampleDetails, BoardState(userStories, new Date(2000)))
  }
  
}
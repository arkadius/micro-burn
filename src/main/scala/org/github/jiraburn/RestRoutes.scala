package org.github.jiraburn

import net.liftweb.common._
import net.liftweb.http.rest.RestHelper

object RestRoutes extends RestHelper {

  serve {
    case Get("rest" :: _, req) =>
      <ala/>
  }

}

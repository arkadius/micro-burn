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
package org.github.microburn

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.webapp.WebAppContext

object Main extends App {

  runServer(None)

  def runServer(optionalResourceBase: Option[String]) = {
    val server = new Server
    val connector = new SelectChannelConnector()
    connector.setPort(ApplicationContext().jettyPort)
    server.addConnector(connector)

    val domain = this.getClass.getProtectionDomain
    val context = new WebAppContext()
    val location = domain.getCodeSource.getLocation
    context.setContextPath("/")
    context.setWar(location.toExternalForm)
    optionalResourceBase.map(context.setResourceBase)
    server.setHandler(context)

    server.start()
    server.join()
  }
}
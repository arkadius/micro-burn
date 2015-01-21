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
package org.github.microburn.integration

import akka.actor.{Actor, ActorRefFactory, ActorSystem, Props}
import akka.io.IO
import akka.testkit.TestProbe
import org.scalatest._
import spray.can.Http
import spray.routing.{HttpService, Route, RoutingSettings}

trait RestIntegrationTest extends BeforeAndAfterAll with MySslConfiguration { self: Suite =>
  protected implicit val system = ActorSystem(getClass.getSimpleName)

  protected implicit val settings = RoutingSettings(system)

  protected def route: Route

  override protected def beforeAll(): Unit = {
    val hostname = "localhost"
    val port = 8088

    class Route extends Actor with HttpService {
      override def receive: Receive = runRoute(route)
      override def actorRefFactory: ActorRefFactory = context
    }

    val routeActor = system.actorOf(Props(new Route))

    val commander = TestProbe()
    commander.send(IO(Http), Http.Bind(routeActor, hostname, port))
    commander.expectMsgType[Http.Bound]
  }

  override protected def afterAll(): Unit = {
    val probe = TestProbe()
    probe.send(IO(Http), Http.CloseAll)
    probe.expectMsg(Http.ClosedAll)
    system.shutdown()
  }

}
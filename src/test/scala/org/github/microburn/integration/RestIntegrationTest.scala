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

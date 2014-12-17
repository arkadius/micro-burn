package org.github.jiraburn.jira

import akka.actor.{Actor, ActorRefFactory, ActorSystem, Props}
import akka.io.IO
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import spray.can.Http
import spray.routing.{HttpService, RoutingSettings}

import scala.concurrent.Await
import scala.concurrent.duration._

class SprintsProviderTest extends Specification with NoTimeConversions {
  val system = ActorSystem(getClass.getSimpleName)

  "get sprints" in new TestSetup {
//    val config = ConfigFactory.parseFile(new File("secret.conf")).withFallback(ConfigFactory.load())
    val config = ConfigFactory.load()
    val jiraConfig = JiraConfig(config)
    val provider = new SprintsProvider(jiraConfig)
    val result = Await.result(provider.allSprints, 5 seconds)
    println(result)
  }

  step {
    implicit val actorSystemImplicit = system
    val probe = TestProbe()
    probe.send(IO(Http), Http.CloseAll)
    probe.expectMsg(Http.ClosedAll)
    system.shutdown()
  }

  class TestSetup extends org.specs2.specification.Scope with MySslConfiguration {

    val hostname = "localhost"
    val port = 8088
    val route = system.actorOf(Props(new Route))

    class Route extends Actor with HttpService {
      implicit val settings = RoutingSettings(system)

      override def receive: Receive = runRoute(
        pathPrefix("jira") {
//        path("/jira/rest/greenhopper/1.0/sprints/56") {
          get {
            getFromFile("src/test/resources/jira/allSprints.json")
          }
        }
      )

      override def actorRefFactory: ActorRefFactory = context
    }

    // automatically bind a server
    val listener = {
      implicit val actorSystemImplicit = system
      val commander = TestProbe()
      commander.send(IO(Http), Http.Bind(route, hostname, port))
      commander.expectMsgType[Http.Bound]
      commander.sender
    }
  }


}

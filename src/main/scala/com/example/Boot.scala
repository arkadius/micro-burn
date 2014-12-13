package com.example

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.client.RequestBuilding
import akka.http.marshallers.xml.ScalaXmlSupport
import akka.http.model.HttpEntity
import akka.http.server._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration._

object Boot extends App with RequestBuilding with ScalaXmlSupport {
  import akka.http.server.Directives._
  import akka.http.server.RouteResult._

  implicit val system = ActorSystem()
  implicit val materializer = FlowMaterializer()

  implicit val routingSetup: RoutingSetup = RoutingSetup(
    routingSettings = RoutingSettings.default,
    executionContext = system.dispatcher,
    flowMaterializer = materializer,
    routingLog = RoutingLog(system.log)
  )

  val serverBinding = Http(system).bind(interface = "localhost", port = 8080)

  serverBinding.startHandlingWith {
    get {
      pathPrefix("") {
        getFromDirectory("src/main/webapp")
      }
    }
  }

  val outgoingFlow = Http(system)
    .outgoingConnection("google.com", 80).flow
    .mapAsyncUnordered(_.entity.toStrict(5 seconds))

  val printingSink = Sink.foreach[HttpEntity.Strict] { response =>
    println("got response: " + response.data.decodeString("utf-8"))
  }

  Source.singleton(Get("/test"))
    .via(outgoingFlow)
    .to(printingSink).run()

}
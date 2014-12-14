package com.example

object Boot extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import dispatch._

  val google = host("google.com") / "test"
  val futureResponse = Http(google OK as.String).either

  for {
    response <- futureResponse
  } {
    println("got: " + response)
  }
}
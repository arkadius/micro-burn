organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % "0.11-SNAPSHOT",
    "com.typesafe.akka" %% "akka-http-xml-experimental" % "0.11-SNAPSHOT",
    "net.liftweb"         %%  "lift-json"     % "2.6-RC2",
    "com.github.tototoshi" %% "scala-csv"     % "1.1.2",
    "org.scalatest"       %%  "scalatest"     % "2.2.2" % "test"
  )
}

Revolver.settings

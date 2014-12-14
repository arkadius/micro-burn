organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val liftV = "3.0-M2"
  Seq(
    "com.typesafe" % "config" % "1.2.1",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "net.liftweb"         %%  "lift-actor"    % liftV,
    "net.liftweb"         %%  "lift-json"     % liftV,
    "com.github.tototoshi" %% "scala-csv"     % "1.1.2",
    "org.scalatest"       %%  "scalatest"     % "2.2.2" % "test"
  )
}

Revolver.settings

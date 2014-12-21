organization  := "org"

version       := "0.1"

scalaVersion  := "2.11.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= {
  val liftV = "3.0-M2"
  val liftEdition = liftV.substring(0,3)
//  val jettyV = "9.3.0.M1" <- jakiś busy waiting się dzieje
  val jettyV = "8.1.16.v20140903"
  Seq(
    "com.typesafe" % "config" % "1.2.1",
    "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.2",
    "net.liftmodules"      %%   s"ng_${liftEdition}" % "0.6.1",
    "net.liftmodules"      %%   s"ng-js_${liftEdition}" % "0.2_1.3.6",
    "net.liftmodules"      %%   s"lift-jquery-module_2.6" % "2.8",
    "net.liftweb"          %%  "lift-webkit"      % liftV,
    "org.eclipse.jetty"     %  "jetty-server"     % jettyV,
    "org.eclipse.jetty"     %  "jetty-webapp"     % jettyV,
    "com.github.tototoshi" %%  "scala-csv"        % "1.1.2",
    "ch.qos.logback"        %  "logback-classic"  % "1.1.2",
    "org.scalatest"        %%  "scalatest"        % "2.2.2" % "test",
    "joda-time"             % "joda-time"         % "2.6" % "test",
    // http mocks:
    "io.spray"             %% "spray-routing"     % "1.3.2" % "test",
    "io.spray"             %% "spray-can"         % "1.3.2" % "test",
    "com.typesafe.akka"    %% "akka-actor"        % "2.3.6" % "test",
    "com.typesafe.akka"    %% "akka-slf4j"        % "2.3.6" % "test",
    "com.typesafe.akka"    %% "akka-testkit"      % "2.3.6" % "test"
  )
}

Revolver.settings

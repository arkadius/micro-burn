import com.banno.license.Plugin.LicenseKeys._
import com.banno.license.Licenses._
import sbtrelease._
import ReleaseStateTransformations._

licenseSettings

license := apache2("Copyright 2015 the original author or authors.")

removeExistingHeaderBlock := true

releaseSettings

ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

organization  := "org.github"

name := "micro-burn"

scalaVersion  := "2.11.4"

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
  "staging"       at "http://oss.sonatype.org/content/repositories/staging",
  "releases"        at "http://oss.sonatype.org/content/repositories/releases",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

parallelExecution in Test := false // żeby port nie był zajmowany prze inne testy

unmanagedResourceDirectories in Compile += { baseDirectory.value / "src/main/webapp" }

seq(net.virtualvoid.sbt.graph.Plugin.graphSettings : _*)

libraryDependencies ++= {
  val liftV = "3.0-M2"
  val liftEdition = liftV.substring(0,3)
//  val jettyV = "9.3.0.M1" <- jakiś busy waiting się dzieje
  val jettyV = "8.1.16.v20140903"
  Seq(
    "com.typesafe" % "config" % "1.2.1",
    "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.2",
    "net.liftmodules"      %%   s"ng_${liftEdition}" % "0.6.2",
    "net.liftmodules"      %%   s"ng-js_${liftEdition}" % "0.2_1.3.8",
    "net.liftmodules"      %%   s"lift-jquery-module_2.6" % "2.8",
    "net.liftweb"          %%  "lift-webkit"      % liftV,
    "org.eclipse.jetty"     %  "jetty-server"     % jettyV,
    "org.eclipse.jetty"     %  "jetty-webapp"     % jettyV,
    "com.github.tototoshi" %%  "scala-csv"        % "1.1.2",
    "ch.qos.logback"        %  "logback-classic"  % "1.1.2",
    "org.scalatest"        %%  "scalatest"        % "2.2.2" % "test",
    "org.scalacheck"       %%  "scalacheck"       % "1.12.1" % "test",
    "joda-time"             % "joda-time"         % "2.6" % "test",
    // http mocks:
    "io.spray"             %% "spray-routing"     % "1.3.2" % "test",
    "io.spray"             %% "spray-can"         % "1.3.2" % "test",
    "com.typesafe.akka"    %% "akka-actor"        % "2.3.6" % "test",
    "com.typesafe.akka"    %% "akka-slf4j"        % "2.3.6" % "test",
    "com.typesafe.akka"    %% "akka-testkit"      % "2.3.6" % "test"
  )
}

assemblyMergeStrategy in assembly := {
  case PathList("scala", "io", _*) => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("org.github.microburn.Main")

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

Revolver.settings

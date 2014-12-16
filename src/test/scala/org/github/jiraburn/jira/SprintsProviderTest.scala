package org.github.jiraburn.jira

import java.io.File

import org.github.jiraburn.TestTags.IntegrationTest
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class SprintsProviderTest extends FlatSpec {

  it should "get sprints" taggedAs IntegrationTest in  {
    val config = ConfigFactory.parseFile(new File("secret.conf")).withFallback(ConfigFactory.load())
    val jiraConfig = JiraConfig(config)
    val provider = new SprintsProvider(jiraConfig)
    val result = Await.result(provider.allSprints, 5 seconds)
    println(result)
  }

}

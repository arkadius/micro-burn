package org.github.micoburn.integration.jira

import com.typesafe.config.Config
import dispatch._

case class JiraConfig(jiraUrl: Req,
                      greenhopperUrl: Req,
                      storyPointsField: String,
                      rapidViewId: Int)

object JiraConfig {
  def apply(config: Config): JiraConfig = {
    JiraConfig(
      jiraUrl           = config.getString("url"),
      greenhopperUrl    = config.getString("greenhopper.url"),
      user              = config.getString("user"),
      password          = config.getString("password"),
      storyPointsField  = config.getString("greenhopper.storyPointsField"),
      rapidViewId       = config.getInt(   "greenhopper.rapidViewId")
    )
  }


  def apply(jiraUrl: String,
            greenhopperUrl: String,
            user: String,
            password: String,
            storyPointsField: String,
            rapidViewId: Int): JiraConfig = {
    JiraConfig(
      jiraUrl           = url(jiraUrl).as_!(user, password),
      greenhopperUrl    = url(greenhopperUrl).as_!(user, password),
      storyPointsField  = storyPointsField,
      rapidViewId       = rapidViewId
    )
  }
}
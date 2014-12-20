package org.github.jiraburn.jira

import com.typesafe.config.Config
import dispatch._

case class JiraConfig(jiraUrl: Req,
                      greenhopperUrl: Req,
                      storyPointsField: String,
                      rapidViewId: Int)

object JiraConfig {
  def apply(config: Config): JiraConfig = {
    JiraConfig(
      jiraUrl           = config.getString("jira.url"),
      greenhopperUrl    = config.getString("jira.greenhopper.url"),
      user              = config.getString("jira.user"),
      password          = config.getString("jira.password"),
      storyPointsField  = config.getString("jira.greenhopper.storyPointsField"),
      rapidViewId       = config.getInt("jira.greenhopper.rapidViewId")
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
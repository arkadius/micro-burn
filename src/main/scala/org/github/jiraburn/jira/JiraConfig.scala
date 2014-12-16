package org.github.jiraburn.jira

import com.typesafe.config.Config
import dispatch._

case class JiraConfig(greenhopperUrl: Req,
                      rapidViewId: Int)

object JiraConfig {
  def apply(config: Config): JiraConfig = {
    JiraConfig(
      config.getString("jira.user"),
      config.getString("jira.password"),
      config.getString("jira.greenhopper.url"),
      config.getInt(   "jira.greenhopper.rapidViewId")
    )
  }


  def apply(user: String,
            password: String,
            greenhopperUrl: String,
            rapidViewId: Int): JiraConfig = {
    JiraConfig(url(greenhopperUrl).as_!(user, password), rapidViewId)
  }
}
package bootstrap.liftweb

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftmodules.JQueryModule
import net.liftweb.actor.MockLiftActor
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.jquery.JQueryArtifacts
import net.liftweb.sitemap.Loc._
import net.liftweb.sitemap._
import org.github.jiraburn.ApplicationContext
import org.github.jiraburn.comet.ChatServer
import org.github.jiraburn.domain.ProjectConfig
import org.github.jiraburn.domain.actors.ProjectActor
import org.github.jiraburn.jira.{TasksDataProvider, JiraConfig, SprintsDataProvider}
import org.github.jiraburn.model.User
import org.github.jiraburn.rest.RestRoutes
import org.github.jiraburn.service.{SprintColumnsHistoryProvider, ProjectUpdater}

import scala.reflect.io.Path


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("org.github.jiraburn")

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.jsArtifacts = JQueryArtifacts
    JQueryModule.InitParam.JQuery=JQueryModule.JQuery172
    JQueryModule.init()

    net.liftmodules.ng.Angular.init()
    net.liftmodules.ng.AngularJS.init()

    LiftSession.afterSessionCreate :+= {(_:LiftSession, req:Req) =>
      ChatServer ! User(req.remoteAddr)
    }

    val context = ApplicationContext()
    context.updater.start()
    LiftRules.statelessDispatch.append(new RestRoutes(context.columnsHistoryProvider))
  }
}
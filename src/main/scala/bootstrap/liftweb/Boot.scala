package bootstrap.liftweb

import java.io.File

import com.typesafe.config.ConfigFactory
import net.liftweb.actor.MockLiftActor
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.sitemap._
import org.github.jiraburn.RestRoutes
import org.github.jiraburn.domain.ProjectConfig
import org.github.jiraburn.domain.actors.ProjectActor

import scala.reflect.io.Path


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("org.github.jiraburn")

    // Build SiteMap
    val entries = List(
      Menu.i("Home") / "index", // the simple way to declare a menu

      // more complex because this menu allows anything in the
      // /static path to be visible
      Menu(Loc("Static", Link(List("static"), matchHead_? = true, "/static/index"),
        "Static Content")))

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQueryArtifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.parseResources("defaults.conf"))
    val projectRoot = new File(config.getString("data.project.root"))
    val projectActor = new ProjectActor(projectRoot, ProjectConfig(config), new MockLiftActor)

    LiftRules.statelessDispatch.append(new RestRoutes(projectActor))

    net.liftmodules.ng.Angular.init()
    net.liftmodules.ng.AngularJS.init()
  }
}
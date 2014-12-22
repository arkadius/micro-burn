import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.webapp.WebAppContext
import org.github.jiraburn.ApplicationContext

object Main extends App {

  runServer(None)

  def runServer(optionalResourceBase: Option[String]) = {
    val server = new Server
    val connector = new SelectChannelConnector()
    connector.setPort(ApplicationContext().jettyPort)
    server.addConnector(connector)

    val domain = this.getClass.getProtectionDomain
    val context = new WebAppContext()
    val location = domain.getCodeSource.getLocation
    context.setContextPath("/")
    context.setWar(location.toExternalForm)
    optionalResourceBase.map(context.setResourceBase)
    server.setHandler(context)

    server.start()
    server.join()
  }
}
import org.eclipse.jetty.server.{NetworkTrafficServerConnector, Server}
import org.eclipse.jetty.webapp.WebAppContext

object Main extends App {
  val server = new Server
  val connector = new NetworkTrafficServerConnector(server)
  connector.setPort(8080)
  server.addConnector(connector)

  val domain = this.getClass.getProtectionDomain
  val context = new WebAppContext()
  val location = domain.getCodeSource.getLocation
  context.setContextPath("/")
  context.setWar(location.toExternalForm)
  context.setResourceBase("src/main/webapp")
  server.setHandler(context)

  server.start()
  server.join()
}
import javax.servlet.ServletContext

import nom.bruno.travelplanner.servlets.{LoginServlet, LogoutServlet, UsersServlet}
import org.scalatra._
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.{Database, DatabaseDef}

class ScalatraBootstrap extends LifeCycle {
  val logger = LoggerFactory.getLogger(getClass)

  var db: DatabaseDef = _

  override def init(context: ServletContext) {
    logger.info("Creating db connection")
    db = Database.forConfig("mysql")
    context.mount(new UsersServlet(db), "/users/*")
    context.mount(new LoginServlet(db), "/login")
    context.mount(new LogoutServlet(db), "/logout")
  }

  private def closeDbConnection(): Unit = {
    logger.info("Closing db connection")
    db.close()
  }

  override def destroy(context: ServletContext) {
    super.destroy(context)
    closeDbConnection
  }
}

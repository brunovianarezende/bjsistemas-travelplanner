import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.{Database, DatabaseDef}
import org.scalatra._
import javax.servlet.ServletContext

import nom.bruno.travelplanner._

class ScalatraBootstrap extends LifeCycle {
  val logger = LoggerFactory.getLogger(getClass)

  var db: DatabaseDef = _

  override def init(context: ServletContext) {
    logger.info("Creating db connection")
    db = Database.forConfig("mysql")
    context.mount(new TravelPlannerServlet(db), "/*")
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

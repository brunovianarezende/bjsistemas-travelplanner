package nom.bruno.travelplanner.servlets

import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json._
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait TravelPlannerServlet extends ScalatraServlet with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  protected implicit def executor: ExecutionContext = global

  val logger = LoggerFactory.getLogger(getClass)

  before() {
    contentType = formats("json")
  }

  error {
    case e: Throwable => {
      logger.error("Error", e)
      halt(InternalServerError())
    }
  }
}

object TravelPlannerServlet {
  def servletInstances(db: Database): Seq[(TravelPlannerServlet, String)] = {
    Seq(
      (new UsersServlet(db), "/users/*"),
      (new LoginServlet(db), "/login"),
      (new LogoutServlet(db), "/logout")
    )
  }

}
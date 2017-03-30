package nom.bruno.travelplanner.controllers

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.utils.Json4sLocalDateSerializer
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json._
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait TravelPlannerStack extends ScalatraFilter with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = TravelPlannerStack.jsonFormats

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

object TravelPlannerStack {
  def servletInstances(db: Database): Seq[(TravelPlannerStack, String)] = {
    Seq(
      (new UsersController(db), "/*"),
      (new TripsController(db), "/*"),
      (new LoginController(db), "/*"),
      (new LogoutController(db), "/*")
    )
  }

  def jsonFormats = new DefaultFormats {
    override val strictOptionParsing = true
  }.withBigDecimal + new EnumNameSerializer(Role) + new Json4sLocalDateSerializer()
}
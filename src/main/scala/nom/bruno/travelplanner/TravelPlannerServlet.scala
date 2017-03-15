package nom.bruno.travelplanner

import org.scalatra._
import org.scalatra.json._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait TravelPlannerServlet extends ScalatraServlet with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  protected implicit def executor: ExecutionContext =  global

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

package nom.bruno.travelplanner

import org.scalatra._
import org.scalatra.json._
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait TravelPlannerStack extends ScalatraServlet with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  protected implicit def executor: ExecutionContext =  global

  before() {
    contentType = formats("json")
  }
}

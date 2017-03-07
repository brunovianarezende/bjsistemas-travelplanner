package nom.bruno.travelplanner

import org.scalatra._
import org.scalatra.json._
import org.json4s.{DefaultFormats, Formats}

trait TravelPlannerStack extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  before() {
    contentType = formats("json")
  }
}

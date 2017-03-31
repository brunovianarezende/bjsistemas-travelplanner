package nom.bruno.travelplanner.controllers

import javax.inject.{Inject, Named}

import com.google.inject.Injector
import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.utils.Json4sLocalDateSerializer
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

trait TravelPlannerStack extends ScalatraFilter with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = TravelPlannerStack.jsonFormats

  @Inject
  @Named("EC") var ec: ExecutionContext = null

  protected implicit def executor: ExecutionContext = ec

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
  def servletInstances(injector: Injector): Seq[(TravelPlannerStack, String)] = {
    Seq(
      (injector.getInstance(classOf[UsersController]), "/*"),
      (injector.getInstance(classOf[TripsController]), "/*"),
      (injector.getInstance(classOf[LoginController]), "/*"),
      (injector.getInstance(classOf[LogoutController]), "/*")
    )
  }

  def jsonFormats = new DefaultFormats {
    override val strictOptionParsing = true
  }.withBigDecimal + new EnumNameSerializer(Role) + new Json4sLocalDateSerializer()
}
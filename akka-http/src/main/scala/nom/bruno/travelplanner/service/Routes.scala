package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import nom.bruno.travelplanner.UserView
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val tempFormat = jsonFormat2(Temp.apply)
}

case class Temp(a: String, b: String)

class Routes @Inject()(@Named("EC") implicit val ec: ExecutionContext) extends JsonProtocol {
  val routes = {
    path("users") {
      get {
        complete(Future {
          Temp("hi", "there")
        })
      }
    }
  }
}

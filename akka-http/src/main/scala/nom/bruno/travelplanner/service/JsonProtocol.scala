package nom.bruno.travelplanner.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.utils.CustomJsonFormats._
import nom.bruno.travelplanner.{ChangeUserData, Error, LoginData, NewUserData, Result, TripView, UserView}
import spray.json.{DefaultJsonProtocol, JsonFormat}

trait JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val roleFormat = jsonEnum(Role)
  implicit val loginDataFormat = jsonFormat2(LoginData.apply)

  implicit val newUserDataFormat = jsonFormat2(NewUserData.apply)
  implicit val errorFormat = jsonFormat1(Error.apply)
  implicit val changeUserDataFormata = jsonFormat3(ChangeUserData.apply)
  implicit val userViewFormat = jsonFormat2(UserView.apply)
  implicit val localDateFormat = jsonLocalDateFormat
  implicit val tripViewFormat = jsonFormat5(TripView.apply)

  implicit def resultFomat[T: JsonFormat] = jsonFormat3(Result.apply[T])
}

package nom.bruno.travelplanner.utils

import java.time.LocalDate

import spray.json.{DeserializationException, JsString, JsValue, JsonFormat}

import scala.util.Try

object CustomJsonFormats {
  def jsonEnum[T <: Enumeration](enu: T) = new JsonFormat[T#Value] {
    def write(obj: T#Value) = JsString(obj.toString)

    def read(json: JsValue) = json match {
      case JsString(txt) if Try(enu.withName(txt)).isSuccess=> enu.withName(txt)
      case something => throw new DeserializationException(s"Expected a value from enum $enu instead of $something")
    }
  }

  def jsonLocalDateFormat = new JsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = {
      json match {
        case JsString(txt) if Try(LocalDate.parse(txt)).isSuccess => LocalDate.parse(txt)
        case value => throw new DeserializationException(s"Can't convert $value to ${classOf[LocalDate]}")
      }
    }

    override def write(obj: LocalDate): JsValue = JsString(obj.toString)
  }
}

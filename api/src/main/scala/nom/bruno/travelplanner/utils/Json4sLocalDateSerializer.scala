package nom.bruno.travelplanner.utils

import java.time.LocalDate

import org.json4s.JsonAST.JString
import org.json4s.{Formats, JValue, Serializer, TypeInfo, MappingException}

import scala.util.Try

class Json4sLocalDateSerializer extends Serializer[LocalDate] {
  private val Class = implicitly[Manifest[LocalDate]].runtimeClass

  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), LocalDate] = {
    case (TypeInfo(Class, _), json) => json match {
      case s: JString if (Try(LocalDate.parse(s.values)).isSuccess) => LocalDate.parse(s.values)
      case value => throw new MappingException(s"Can't convert $value to $Class")
    }
  }

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case i: LocalDate => JString(i.toString)
  }
}

package nom.bruno.travelplanner.utils

import java.time.LocalDate

import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import org.json4s.{DefaultFormats, Formats, MappingException}
import org.scalatest.FunSuite

class Json4sLocalDateSerializerTest extends FunSuite {
  implicit val formats: Formats = DefaultFormats + new Json4sLocalDateSerializer()

  test("serialization") {
    assert(write(LocalDate.of(2012, 3, 1)) == "\"2012-03-01\"")
  }

  test("deserialization") {
    assert(parse("[\"2012-03-01\"]").extract[Seq[LocalDate]] == Seq(LocalDate.of(2012, 3, 1)))
  }

  test("deserialization exception") {
    assertThrows[MappingException] {
      parse("[\"I'm not a date!\"]").extract[Seq[LocalDate]]
    }
  }
}

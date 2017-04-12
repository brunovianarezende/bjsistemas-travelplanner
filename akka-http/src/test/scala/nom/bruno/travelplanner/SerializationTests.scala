package nom.bruno.travelplanner

import java.time.LocalDate

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.service.JsonProtocol
import org.scalatest.FunSpec
import spray.json._

class SerializationTests extends FunSpec with JsonProtocol {
  describe("ChangeUserData") {
    it("must correctly deserialize roles") {
      assert("{\"role\": \"NORMAL\"}".parseJson.convertTo[ChangeUserData] == ChangeUserData.create(Role.NORMAL))
    }
    it("must correctly serialize roles") {
      assert(ChangeUserData.create(Role.NORMAL).toJson.toString == "{\"role\":\"NORMAL\"}")
    }
    it("must throw an exception for invalid roles instead of ignoring the role") {
      assertThrows[DeserializationException] {
        "{\"role\": \"BLABLA\"}".parseJson.convertTo[ChangeUserData]
      }
    }
  }

  describe("Enum") {
    it("deserialization") {
      assert("""["NORMAL"]""".parseJson.convertTo[Seq[Role.Role]] == Seq(Role.NORMAL))
    }
    it("serialization") {
      ChangeUserData.create(Role.NORMAL).toJson.toString
      assert(Seq(Role.NORMAL).toJson.toString == """["NORMAL"]""")
    }
    it("deserialization exception") {
      assertThrows[DeserializationException] {
        """["BLABLA"]""".parseJson.convertTo[Seq[Role.Role]]
      }
    }
  }

  describe("LocalDate") {
    it("serialization") {
      assert(LocalDate.of(2012, 3, 1).toJson.toString == "\"2012-03-01\"")
    }

    it("deserialization") {
      assert("[\"2012-03-01\"]".parseJson.convertTo[Seq[LocalDate]] == Seq(LocalDate.of(2012, 3, 1)))
    }

    it("deserialization exception") {
      assertThrows[DeserializationException] {
        "[\"I'm not a date!\"]".parseJson.convertTo[Seq[LocalDate]]
      }
    }
  }
}

package nom.bruno.travelplanner

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.service.JsonProtocol
import org.scalatest.FunSpec
import spray.json._

class SerializationTests extends FunSpec with JsonProtocol {
  describe("ChangeUserData") {
    it("must correctly deserialize roles") {
      assert("""{"role": "NORMAL"}""".parseJson.convertTo[ChangeUserData] == ChangeUserData.create(Role.NORMAL))
    }
    it("must correctly serialize roles") {
      ChangeUserData.create(Role.NORMAL).toJson.toString
      assert(ChangeUserData.create(Role.NORMAL).toJson.toString == """{"role":"NORMAL"}""")
    }
    it("must throw an exception for invalid roles") {
      assertThrows[DeserializationException] {
        """{"role": "BLABLA"}""".parseJson.convertTo[ChangeUserData]
      }
    }
  }
}

package nom.bruno.travelplanner.unit

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.servlets.{ChangeUserData, TravelPlannerServlet}
import org.scalatest.FunSpec
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

class SerializationTests extends FunSpec {
  describe("deserialize ChangeUserData") {
    implicit val jsonFormats: Formats = TravelPlannerServlet.jsonFormats

    it("must correctly deserialize roles") {
      assert(parse("{\"role\": \"NORMAL\"}").extract[ChangeUserData] == ChangeUserData.create(Role.NORMAL))
    }
    it("must correctly serialize roles") {
      assert(write(ChangeUserData.create(Role.NORMAL)) == "{\"role\":\"NORMAL\"}")
    }
    it("must throw an exception for invalid roles") {
      assertThrows[MappingException] {
        parse("{\"role\": \"BLABLA\"}").extract[ChangeUserData]
      }
    }
  }

}

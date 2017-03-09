package nom.bruno.travelplanner

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}

class UsersServletTests extends BaseTravelPlannerServletTest {
  addServlet(new UsersServlet(db), "/users/*")

  feature("get all users") {
    scenario("no user in db") {
      get("/users") {
        status should equal (200)
        implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

        parse(body).extract[List[_]] should be (List())
      }
    }
  }
}
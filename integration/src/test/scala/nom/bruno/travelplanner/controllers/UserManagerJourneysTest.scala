package nom.bruno.travelplanner.controllers

import java.net.HttpCookie

import nom.bruno.travelplanner.Tables.Role
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.GivenWhenThen

class UserManagerJourneysTest extends BaseIntegrationTravelPlannerStackTest with GivenWhenThen {
  feature("user manager journeys") {
    scenario("user manager changes data of a normal user") {
      withUsers {
        Given("a user manager logged in the site")
        val sessionId = postAsJson("/login", LoginData(USER_MANAGER2, PASSWORD)) {
          status should equal(200)
          val cookies = header.get("Set-Cookie")
          HttpCookie.parse(cookies.get).get(0).getValue
        }

        val headers = authHeader(sessionId)

        Then("he must be able to get info about a normal user")
        get(s"/users/$NORMAL1", headers = headers) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }

        And("he must also be able to change the role of the normal user")
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create(Role.USER_MANAGER), headers = headers) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true)
          )
        }
        get(s"/users/$NORMAL1", headers = headers) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.USER_MANAGER)))
          )
        }
      }
    }
  }
}
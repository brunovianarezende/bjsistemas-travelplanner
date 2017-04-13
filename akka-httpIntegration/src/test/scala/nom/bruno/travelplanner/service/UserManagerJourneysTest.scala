package nom.bruno.travelplanner.service

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.{ChangeUserData, LoginData, Result, UserView}
import org.scalatest.GivenWhenThen

class UserManagerJourneysTest extends BaseIntegrationRoutesTest with GivenWhenThen {
  feature("user manager journeys") {
    scenario("user manager changes data of a normal user") {
      withUsers {
        Given("a user manager logged in the site")
        val sessionId = Post("/login", LoginData(USER_MANAGER2, PASSWORD)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          val cookie = getCookie
          cookie.value
        }

        val authenticationHeader = authHeader(sessionId)

        Then("he must be able to Get info about a normal user")
        Get(s"/users/$NORMAL1").addHeader(authenticationHeader) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }

        And("he must also be able to change the role of the normal user")
        Post(s"/users/$NORMAL1", ChangeUserData.create(Role.USER_MANAGER)).addHeader(authenticationHeader) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true)
          )
        }
        Get(s"/users/$NORMAL1").addHeader(authenticationHeader) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.USER_MANAGER)))
          )
        }
      }
    }
  }
}
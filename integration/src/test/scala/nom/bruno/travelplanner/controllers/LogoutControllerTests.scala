package nom.bruno.travelplanner.controllers

import nom.bruno.travelplanner.services.AuthenticationService
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LogoutControllerTests extends BaseTravelPlannerStackTest {
  feature("logout") {
    scenario("successful logout") {
      withUsers {
        post("/logout", headers = authHeaderFor(ADMIN1)) {
          status should equal(200)
          val result = parse(body).extract[Result[_]]
          result.success should be(true)
          val authenticationService = new AuthenticationService(db)
          Await.result(authenticationService.getSessionUser(xSessionIdFor(ADMIN1)), Duration.Inf) should be(None)
        }
      }
    }
  }
}

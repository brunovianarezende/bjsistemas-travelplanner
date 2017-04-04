package nom.bruno.travelplanner.controllers

import org.json4s.jackson.JsonMethods.parse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LogoutControllerTests extends BaseApiTravelPlannerStackTest {
  feature("logout") {
    scenario("successful logout") {
      withUsers {
        when(authenticationService.deleteSession(any())).thenReturn(Future {
          1
        })
        post("/logout", headers = authHeaderFor(ADMIN1)) {
          status should equal(200)
          val result = parse(body).extract[Result[_]]
          result.success should be(true)
          verify(authenticationService, times(1)).deleteSession(any())
        }
      }
    }
  }
}

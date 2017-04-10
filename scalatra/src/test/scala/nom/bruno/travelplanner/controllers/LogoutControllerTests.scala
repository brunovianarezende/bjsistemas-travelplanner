package nom.bruno.travelplanner.controllers

import nom.bruno.travelplanner.Result
import org.json4s.jackson.JsonMethods.parse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LogoutControllerTests extends BaseApiTravelPlannerStackTest {
  feature("logout") {
    scenario("successful logout") {
      withUsers {
        when(usersService.finishSession(any())).thenReturn(Future {})
        post("/logout", headers = authHeaderFor(ADMIN1)) {
          status should equal(200)
          val result = parse(body).extract[Result[_]]
          result.success should be(true)
          verify(usersService, times(1)).finishSession(any())
        }
      }
    }
  }
}

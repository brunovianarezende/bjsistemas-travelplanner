package nom.bruno.travelplanner.service

import nom.bruno.travelplanner.Result
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.concurrent.Future

class LogoutRouteTest extends BaseRoutesTest {
  feature("logout") {
    scenario("successful logout") {
      withUsers {
        when(usersService.finishSession(any())).thenReturn(Future {})
        Post("/logout").addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          val result = responseAs[Result[Unit]]
          result.success should be(true)
          verify(usersService, times(1)).finishSession(any())
        }
      }
    }
  }
}

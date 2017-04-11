package nom.bruno.travelplanner.service

import akka.http.scaladsl.model.headers.{HttpCookie, `Set-Cookie`}
import nom.bruno.travelplanner.{Error, ErrorCodes, LoginData, Result}
import org.mockito.Mockito._
import spray.json.pimpAny

import scala.concurrent.Future

class LoginRouteTest extends BaseRoutesTest {
  feature("login") {
    scenario("successful login") {
      withUsers {
        when(usersService.loginUser(NORMAL2, PASSWORD)).thenReturn(Future {
          Some("a-session-id")
        })
        Post("/login", LoginData(NORMAL2, PASSWORD)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          val result = responseAs[Result[Unit]]
          result.success should be(true)
          header[`Set-Cookie`] shouldEqual (Some(`Set-Cookie`(HttpCookie("X-Session-Id", value = "a-session-id"))))
        }
      }
    }

    scenario("wrong password") {
      withUsers {
        val data = LoginData(NORMAL1, "wrongpassword")
        when(usersService.loginUser(data.email, data.password)).thenReturn(Future {
          None
        })
        Post("/login", data) ~>
          routesService.routes ~> check {
          status.intValue should equal(401)
          val result = responseAs[Result[Unit]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
        }
      }
    }

    scenario("email doesn't exist") {
      val data = LoginData("brunore@email.com", "apassword")
      when(usersService.loginUser(data.email, data.password)).thenReturn(Future {
        None
      })
      Post("/login", data) ~>
        routesService.routes ~> check {
        status.intValue should equal(401)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
      }
    }

    scenario("bad schema") {
      for (badInput <- List(Map.empty[String, String].toJson,
        List.empty[String].toJson,
        (1).toJson,
        Map("password" -> "!1APassword").toJson,
        Map("email" -> "brunore@email.com").toJson)) {
        Post("/login", badInput) ~>
          routesService.routes ~> check {
          status.intValue should equal(400)
          val result = responseAs[Result[Unit]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }
  }

}

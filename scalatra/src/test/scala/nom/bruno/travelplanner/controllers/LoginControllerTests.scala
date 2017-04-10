package nom.bruno.travelplanner.controllers

import java.net.HttpCookie

import nom.bruno.travelplanner.{Error, ErrorCodes, LoginData, Result}
import org.json4s.jackson.JsonMethods.parse
import org.mockito.Mockito._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LoginControllerTests extends BaseApiTravelPlannerStackTest {
  feature("login") {
    scenario("successful login") {
      withUsers {
        when(usersService.loginUser(NORMAL2, PASSWORD)).thenReturn(Future {
          Some("a session id")
        })
        postAsJson("/login", LoginData(NORMAL2, PASSWORD)) {
          status should equal(200)
          val result = parse(body).extract[Result[_]]
          result.success should be(true)
          val cookies = header.get("Set-Cookie")
          cookies should not be (None)
          val httpCookies = HttpCookie.parse(cookies.get)
          httpCookies.size should be(1)
          httpCookies.get(0).getName should be("X-Session-Id")
        }
      }
    }

    scenario("wrong password") {
      withUsers {
        val data = LoginData(NORMAL1, "wrongpassword")
        when(usersService.loginUser(data.email, data.password)).thenReturn(Future {
          None
        })
        postAsJson("/login", data) {
          status should equal(401)
          val result = parse(body).extract[Result[_]]
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
      postAsJson("/login", data) {
        status should equal(401)
        val result = parse(body).extract[Result[_]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
      }
    }

    scenario("bad schema") {
      for (badInput <- List(Map(),
        List(),
        (1).asInstanceOf[AnyRef],
        Map("password" -> "!1APassword"),
        Map("email" -> "brunore@email.com"))) {
        postAsJson("/login", badInput) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }
  }

}

package nom.bruno.travelplanner

import java.net.HttpCookie

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.controllers._
import org.json4s.jackson.JsonMethods.parse

class LoginControllerTests extends BaseTravelPlannerStackTest {
  feature("login") {
    scenario("successful login") {
      withUsers {
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

    scenario("make sure salt is applied when doing comparison") {
      val userWithNoSalt = Tables.User(None, "ble@bla.com", "passsword", "salt", Role.NORMAL)
      withAdditionalUsers(Seq(userWithNoSalt)) {
        postAsJson("/login", LoginData("ble@bla.com", PASSWORD)) {
          status should equal(401)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
        }
      }
    }

    scenario("wrong password") {
      withUsers {
        postAsJson("/login", LoginData(NORMAL1, "wrongpassword")) {
          status should equal(401)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
        }
      }
    }

    scenario("email doesn't exist") {
      postAsJson("/login", LoginData("brunore@email.com", "apassword")) {
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

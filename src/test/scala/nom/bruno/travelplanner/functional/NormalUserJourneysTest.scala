package nom.bruno.travelplanner.functional

import java.net.HttpCookie

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.servlets._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.GivenWhenThen

class NormalUserJourneysTest extends BaseTravelPlannerServletTest with GivenWhenThen {
  feature("user journey") {
    scenario("login, get users, logout, get users again") {
      val email = "brunore@email.com"
      val password = "apassword"

      When("a user registers itself")
      putAsJson(s"/users/$email", NewUserData(password, password)) {
        status should equal(200)
        parse(body).extract[Result[_]].success should be(true)
      }

      And("login in the site")
      val sessionId = postAsJson("/login", LoginData(email, password)) {
        status should equal(200)
        val result = parse(body).extract[Result[_]]
        result.success should be(true)
        val cookies = header.get("Set-Cookie")
        cookies should not be (None)
        val httpCookies = HttpCookie.parse(cookies.get)
        httpCookies.size should be(1)
        val cookie = httpCookies.get(0)
        cookie.getName should be("X-Session-Id")
        cookie.getValue
      }

      val authHeader = Map("Cookie" -> s"X-Session-Id=$sessionId")

      Then("he must be able to get info about his user")
      get(s"/users/$email", headers = authHeader) {
        status should equal(200)

        parse(body).extract[Result[UserView]] should have(
          'success (true),
          'data (Some(UserView(email, Role.NORMAL)))
        )
      }

      When("he logout")
      post("/logout", headers = authHeader) {
        status should equal(200)
        val result = parse(body).extract[Result[_]]
        result.success should be(true)
      }

      Then("he must not be able to get info about his user anymore")
      get(s"/users/$email", headers = authHeader) {
        status should equal(401)
        parse(body).extract[Result[UserView]] should have(
          'success (false),
          'errors (Some(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED))))
        )
      }
    }
  }
}
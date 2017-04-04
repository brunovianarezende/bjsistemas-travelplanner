package nom.bruno.travelplanner.controllers

import java.net.HttpCookie

import nom.bruno.travelplanner.Tables.Role
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.GivenWhenThen

class NormalUserJourneysTest extends BaseIntegrationTravelPlannerStackTest with GivenWhenThen {
  feature("user journey") {
    scenario("login, get user, logout, get user again") {
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

      val headers = authHeader(sessionId)

      Then("he must be able to get info about his user")
      get(s"/users/$email", headers = headers) {
        status should equal(200)

        parse(body).extract[Result[UserView]] should have(
          'success (true),
          'data (Some(UserView(email, Role.NORMAL)))
        )
      }

      When("he logout")
      post("/logout", headers = headers) {
        status should equal(200)
        val result = parse(body).extract[Result[_]]
        result.success should be(true)
      }

      Then("he must not be able to get info about his user anymore")
      get(s"/users/$email", headers = headers)(checkNotAuthenticatedError)
    }
  }

  scenario("login, change password and login again") {
    val email = "brunore@email.com"
    val password = "apassword"
    val newPassword = "newpassword"

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

    Then("he can change its password")
    val headers = authHeader(sessionId)
    postAsJson(s"/users/$email", ChangeUserData.create(newPassword, newPassword), headers = headers) {
      status should equal(200)

      parse(body).extract[Result[UserView]] should have(
        'success (true)
      )
    }

    When("he logout")
    post("/logout", headers = headers) {
      status should equal(200)
      val result = parse(body).extract[Result[_]]
      result.success should be(true)
    }

    Then("he must not be able to login with old password")
    postAsJson("/login", LoginData(email, password)) {
      status should equal(401)
      val result = parse(body).extract[Result[_]]
      result.success should be(false)
      result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
    }

    And("he must be able to login with new password")
    postAsJson("/login", LoginData(email, newPassword)) {
      status should equal(200)
    }
  }
}
package nom.bruno.travelplanner.service

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner._
import org.scalatest.GivenWhenThen

class NormalUserJourneysTest extends BaseIntegrationRoutesTest with GivenWhenThen {
  feature("user journey") {
    scenario("login, get user, logout, get user again") {
      val email = "brunore@email.com"
      val password = "apassword"

      When("a user registers itself")
      Put(s"/users/$email", NewUserData(password, password)) ~>
        routesService.routes ~> check {
        status.intValue should equal(200)
        responseAs[Result[Unit]].success should be(true)
      }

      And("login in the site")
      val sessionId = Post("/login", LoginData(email, password)) ~>
        routesService.routes ~> check {
        status.intValue should equal(200)
        val result = responseAs[Result[Unit]]
        result.success should be(true)
        val cookie = getCookie
        cookie.name should be("X-Session-Id")
        cookie.value
      }

      val authenticationHeader = authHeader(sessionId)

      Then("he must be able to get info about his user")
      Get(s"/users/$email").addHeader(authenticationHeader) ~>
        routesService.routes ~> check {
        status.intValue should equal(200)

        responseAs[Result[UserView]] should have(
          'success (true),
          'data (Some(UserView(email, Role.NORMAL)))
        )
      }

      When("he logout")
      Post("/logout").addHeader(authenticationHeader) ~>
        routesService.routes ~> check {
        status.intValue should equal(200)
        val result = responseAs[Result[Unit]]
        result.success should be(true)
      }

      Then("he must not be able to get info about his user anymore")
      Get(s"/users/$email").addHeader(authenticationHeader) ~>
        routesService.routes ~> check(checkNotAuthenticatedError)
    }
  }

  scenario("login, change password and login again") {
    val email = "brunore@email.com"
    val password = "apassword"
    val newPassword = "newpassword"

    When("a user registers itself")
    Put(s"/users/$email", NewUserData(password, password)) ~>
      routesService.routes ~> check {
      status.intValue should equal(200)
      responseAs[Result[Unit]].success should be(true)
    }

    And("login in the site")
    val sessionId = Post("/login", LoginData(email, password)) ~>
      routesService.routes ~> check {
      status.intValue should equal(200)
      val result = responseAs[Result[Unit]]
      result.success should be(true)
      val cookie = getCookie
      cookie.name should be("X-Session-Id")
      cookie.value
    }

    Then("he can change its password")
    val authenticationHeader = authHeader(sessionId)
    Post(s"/users/$email", ChangeUserData.create(newPassword, newPassword)).addHeader(authenticationHeader) ~>
      routesService.routes ~> check {
      status.intValue should equal(200)

      responseAs[Result[UserView]] should have(
        'success (true)
      )
    }

    When("he logout")
    Post("/logout").addHeader(authenticationHeader) ~>
      routesService.routes ~> check {
      status.intValue should equal(200)
      val result = responseAs[Result[Unit]]
      result.success should be(true)
    }

    Then("he must not be able to login with old password")
    Post("/login", LoginData(email, password)) ~>
      routesService.routes ~> check {
      status.intValue should equal(401)
      val result = responseAs[Result[Unit]]
      result.success should be(false)
      result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
    }

    And("he must be able to login with new password")
    Post("/login", LoginData(email, newPassword)) ~>
      routesService.routes ~> check {
      status.intValue should equal(200)
    }
  }
}
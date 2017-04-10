package nom.bruno.travelplanner.service

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import nom.bruno.travelplanner.Tables.{Role, User}
import nom.bruno.travelplanner._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{when, _}
import org.scalatest.FeatureSpec

import scala.concurrent.Future

class UserRoutesTest extends BaseRoutesTest {
  feature("add users") {
    scenario("all ok") {
      when(usersService.addUser(any())).thenReturn(Future {})
      when(usersService.getUser(any())).thenReturn(Future {
        None
      })
      Put("/users/brunore@email.com", NewUserData("apassword", "apassword")) ~> routesService.routes ~> check {
        status.intValue should equal(200)
        responseAs[Result[Unit]].success should be(true)
        val captor: ArgumentCaptor[User] = ArgumentCaptor.forClass(classOf[User])
        verify(usersService, times(1)).addUser(captor.capture())
        captor.getValue.email should be("brunore@email.com")
      }
    }

    scenario("bad schema") {
      for (badInput <- List(
        """{}""",
        """[]""",
        """1""",
        """{"password": "!1APassword"}""",
        """{"password_confirmation": "!1APassword"}"""
      )) {
        Put("/users/brunore@email.com", HttpEntity(`application/json`, badInput)) ~>
          routesService.routes ~> check {
          status.intValue should equal(400)
          val result = responseAs[Result[Unit]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }

    scenario("invalid password") {
      Put("/users/brunore@email.com", NewUserData("abc", "abc")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD)))
      }
    }

    scenario("wrong confirmation") {
      Put("/users/brunore@email.com", NewUserData("abcdefg", "1234567")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
      }
    }

    scenario("invalid email") {
      Put("/users/brunore", NewUserData("apassword", "apassword")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_EMAIL)))
      }
    }

    scenario("user already registered") {
      when(usersService.getUser(any())).thenReturn(Future {
        Some(User.withSaltedPassword("brunore@email.com", "apassword"))
      })

      Put("/users/brunore@email.com", NewUserData("apassword", "apassword")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.USER_ALREADY_REGISTERED)))
      }
    }
  }

  feature("get one user by email") {
    scenario("user not authenticated") {
      Get(s"/users/$NORMAL1") ~>
        routesService.routes ~> check(checkNotAuthenticatedError)
    }

    scenario("get own user") {
      withUsers {
        Get(s"/users/$NORMAL1").addHeader(authHeaderFor(NORMAL1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }
        Get(s"/users/$USER_MANAGER1").addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(USER_MANAGER1, Role.USER_MANAGER)))
          )
        }
        Get(s"/users/$ADMIN1").addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(ADMIN1, Role.ADMIN)))
          )
        }
      }
    }

    scenario("get other user") {
      withUsers {
        // for more detailed rules, please see nom.bruno.travelplanner.unit.UserTests#can see
        Get(s"/users/$ADMIN1").addHeader(authHeaderFor(NORMAL1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(403)

          responseAs[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
        Get(s"/users/$NORMAL1").addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }
        Get(s"/users/$ADMIN1").addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(403)

          responseAs[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }

        Get(s"/users/$USER_MANAGER1").addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)

          responseAs[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(USER_MANAGER1, Role.USER_MANAGER)))
          )
        }
      }
    }

    scenario("email not found") {
      withUsers {
        Get("/users/idontexist@bla.com").addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(404)
          responseAs[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }
  }
}

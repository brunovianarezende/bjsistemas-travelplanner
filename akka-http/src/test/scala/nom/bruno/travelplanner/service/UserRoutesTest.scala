package nom.bruno.travelplanner.service

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import nom.bruno.travelplanner.Tables.{Role, User}
import nom.bruno.travelplanner._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{when, _}
import spray.json.pimpAny

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

  feature("change user data") {
    // for more detailed permission rules, please see nom.bruno.travelplanner.unit.UserTests#change role or password
    scenario("a user can change its own password") {
      withUsers {
        when(usersService.updateUser(any(), any(), any())).thenReturn(Future {
          1
        })
        Post(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword")).addHeader(authHeaderFor(NORMAL1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          responseAs[Result[Unit]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("a user can't change its own role") {
      withUsers {
        Post(s"/users/$USER_MANAGER1", ChangeUserData.create(Role.NORMAL)).addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(403)
          responseAs[Result[Unit]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.CANT_CHANGE_ROLE))))
          )
        }
      }
    }

    scenario("a user manager can change normal users role's and password's") {
      withUsers {
        when(usersService.updateUser(any(), any(), any())).thenReturn(Future {
          1
        })
        Post(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword", Role.USER_MANAGER)).addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          responseAs[Result[Unit]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("a user manager can't change admins' role") {
      withUsers {
        Post(s"/users/$ADMIN1", ChangeUserData.create(Role.USER_MANAGER)).addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(403)
          responseAs[Result[Unit]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.CANT_CHANGE_ROLE))))
          )
        }
      }
    }

    scenario("a user manager can't change admins' password") {
      withUsers {
        Post(s"/users/$ADMIN1", ChangeUserData.create("newpassword", "newpassword")).addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(403)
          responseAs[Result[Unit]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.CANT_CHANGE_PASSWORD))))
          )
        }
      }
    }

    scenario("an admin can change other users role's and password's") {
      withUsers {
        when(usersService.updateUser(any(), any(), any())).thenReturn(Future {
          1
        })
        Post(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword", Role.USER_MANAGER)).addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          responseAs[Result[Unit]] should have(
            'success (true)
          )
        }
        Post(s"/users/$USER_MANAGER1", ChangeUserData.create("newpassword", "newpassword", Role.ADMIN)).addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(200)
          responseAs[Result[Unit]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("invalid new password") {
      withUsers {
        Post(s"/users/$NORMAL1", ChangeUserData.create("abc", "abc")).addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(400)
          val result = responseAs[Result[Unit]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD)))
        }
      }
    }

    scenario("wrong confirmation") {
      withUsers {
        Post(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "jkjkjkjjkjkjk")).addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(400)
          val result = responseAs[Result[Unit]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
        }
      }
    }

    scenario("user not authenticated") {
      withUsers {
        Post(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "jkjkjkjjkjkjk")) ~>
          routesService.routes ~> check(checkNotAuthenticatedError)
      }
    }

    scenario("bad schema") {
      withUsers {
        for (badInput <- List(ChangeUserData(None, None, None).toJson,
          ChangeUserData(Some("password"), None, None).toJson,
          ChangeUserData(None, Some("confirmation"), None).toJson,
          Map("role" -> "NEW_ROLE").toJson)) {
          Post(s"/users/$NORMAL1", badInput).addHeader(authHeaderFor(ADMIN1)) ~>
            routesService.routes ~> check {
            assert(status.intValue() == 400, badInput)
            status.intValue should equal(400)
            val result = responseAs[Result[Unit]]
            result.success should be(false)
            result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
          }
        }
      }
    }

    scenario("email not found") {
      withUsers {
        Post(s"/users/idontexist@bla.com", ChangeUserData.create("newpassword", "newpassword")).addHeader(authHeaderFor(ADMIN1)) ~>
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

  feature("delete user") {
    // for more detailed permission rules, please see nom.bruno.travelplanner.unit.UserTests#delete
    scenario("no user can delete itself") {
      withUsers {
        for (user <- Seq(ADMIN1, USER_MANAGER1, NORMAL1)) {
          Delete(s"/users/$user").addHeader(authHeaderFor(user)) ~>
            routesService.routes ~> check {
            status.intValue should be(403)
            responseAs[Result[Unit]] should have(
              'success (false),
              'errors (Some(List(Error(ErrorCodes.CANT_DELETE_USER))))
            )
          }
        }
      }
    }

    scenario("a normal user can't delete any other user") {
      withUsers {
        for (user <- Seq(ADMIN1, USER_MANAGER1, NORMAL2)) {
          Delete(s"/users/$user").addHeader(authHeaderFor(NORMAL1)) ~>
            routesService.routes ~> check {
            status.intValue should be(403)
            responseAs[Result[Unit]] should have(
              'success (false),
              'errors (Some(List(Error(ErrorCodes.CANT_DELETE_USER))))
            )
          }
        }
      }
    }

    scenario("a user manager can delete normal users") {
      withUsers {
        when(usersService.deleteUser(any())).thenReturn(Future {
          1
        })
        Delete(s"/users/$NORMAL1").addHeader(authHeaderFor(USER_MANAGER1)) ~>
          routesService.routes ~> check {
          status.intValue should be(200)
          responseAs[Result[Unit]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("an admin can delete normal and user manager users") {
      withUsers {
        when(usersService.deleteUser(any())).thenReturn(Future {
          1
        })
        for (user <- Seq(NORMAL1, USER_MANAGER1)) {
          Delete(s"/users/$user").addHeader(authHeaderFor(ADMIN1)) ~>
            routesService.routes ~> check {
            status.intValue should be(200)
            responseAs[Result[Unit]] should have(
              'success (true)
            )
          }
        }
      }
    }

    scenario("user not authenticated") {
      Delete(s"/users/$NORMAL1") ~>
        routesService.routes ~> check(checkNotAuthenticatedError)
    }

    scenario("try to delete user that doesn't exist") {
      withUsers {
        Delete("/users/idontexist@users.com").addHeader(authHeaderFor(ADMIN1)) ~>
          routesService.routes ~> check {
          status.intValue should equal(404)
          responseAs[Result[Unit]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }
  }

}

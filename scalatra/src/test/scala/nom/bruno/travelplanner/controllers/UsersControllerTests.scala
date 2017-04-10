package nom.bruno.travelplanner.controllers

import nom.bruno.travelplanner.Tables.{Role, User}
import nom.bruno.travelplanner._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UsersControllerTests extends BaseApiTravelPlannerStackTest {
  feature("add users") {
    scenario("all ok") {
      when(usersService.addUser(any())).thenReturn(Future {})
      when(usersService.getUser(any())).thenReturn(Future {
        None
      })
      putAsJson("/users/brunore@email.com", NewUserData("apassword", "apassword")) {
        status should equal(200)
        parse(body).extract[Result[_]].success should be(true)
        verify(usersService, times(1)).getUser(any())
        val captor: ArgumentCaptor[User] = ArgumentCaptor.forClass(classOf[User])
        verify(usersService, times(1)).addUser(captor.capture())
        captor.getValue.email should be("brunore@email.com")
      }
    }

    scenario("bad schema") {
      for (badInput <- List(Map(),
        List(),
        (1).asInstanceOf[AnyRef],
        Map("password" -> "!1APassword"),
        Map("password_confirmation" -> "!1APassword"))) {
        putAsJson("/users/brunore@email.com", badInput) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }

    scenario("invalid password") {
      putAsJson("/users/brunore@email.com", NewUserData("abc", "abc")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD)))
      }
    }

    scenario("wrong confirmation") {
      putAsJson("/users/brunore@email.com", NewUserData("abcdefg", "1234567")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
      }
    }

    scenario("invalid email") {
      putAsJson("/users/brunore", NewUserData("apassword", "apassword")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_EMAIL)))
      }
    }

    scenario("user already registered") {
      when(usersService.getUser(any())).thenReturn(Future {
        Some(User.withSaltedPassword("brunore@email.com", "apassword"))
      })

      putAsJson("/users/brunore@email.com", NewUserData("apassword", "apassword")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.USER_ALREADY_REGISTERED)))
      }
    }
  }

  feature("get all users") {
    scenario("user authenticated") {
      withUsers {
        when(usersService.getUsersVisibleFor(u(ADMIN1))).thenReturn(Future {
          ALL_USERS.sortBy(_.email)
        })

        get("/users", headers = authHeaderFor(ADMIN1)) {
          status should equal(200)

          parse(body).extract[Result[List[UserView]]].data should be(Some(List(
            UserView(ADMIN1, Role.ADMIN),
            UserView(ADMIN2, Role.ADMIN),
            UserView(USER_MANAGER1, Role.USER_MANAGER),
            UserView(USER_MANAGER2, Role.USER_MANAGER),
            UserView(NORMAL1, Role.NORMAL),
            UserView(NORMAL2, Role.NORMAL)
          ).sortBy(_.email)))
        }
      }
    }

    scenario("user not authenticated - no header") {
      get("/users")(checkNotAuthenticatedError)
    }

    scenario("user not authenticated - expired header") {
      when(usersService.getSessionUser(any())).thenReturn(Future {
        None
      })
      get("/users", headers = authHeaderFor(ADMIN1))(checkNotAuthenticatedError)
    }
  }

  feature("get one user by email") {
    scenario("user not authenticated") {
      get(s"/users/$NORMAL1")(checkNotAuthenticatedError)
    }

    scenario("get own user") {
      withUsers {
        get(s"/users/$NORMAL1", headers = authHeaderFor(NORMAL1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }
        get(s"/users/$USER_MANAGER1", headers = authHeaderFor(USER_MANAGER1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(USER_MANAGER1, Role.USER_MANAGER)))
          )
        }
        get(s"/users/$ADMIN1", headers = authHeaderFor(ADMIN1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(ADMIN1, Role.ADMIN)))
          )
        }
      }
    }

    scenario("get other user") {
      withUsers {
        // for more detailed rules, please see nom.bruno.travelplanner.unit.UserTests#can see
        get(s"/users/$ADMIN1", headers = authHeaderFor(NORMAL1)) {
          status should equal(403)

          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
        get(s"/users/$NORMAL1", headers = authHeaderFor(USER_MANAGER1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }
        get(s"/users/$ADMIN1", headers = authHeaderFor(USER_MANAGER1)) {
          status should equal(403)

          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
        get(s"/users/$USER_MANAGER1", headers = authHeaderFor(ADMIN1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(USER_MANAGER1, Role.USER_MANAGER)))
          )
        }
      }
    }

    scenario("email not found") {
      withUsers {
        get("/users/idontexist@bla.com", headers = authHeaderFor(ADMIN1)) {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have(
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
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword"), authHeaderFor(NORMAL1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("a user can't change its own role") {
      withUsers {
        postAsJson(s"/users/$USER_MANAGER1", ChangeUserData.create(Role.NORMAL), authHeaderFor(USER_MANAGER1)) {
          status should equal(403)
          parse(body).extract[Result[_]] should have(
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
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword", Role.USER_MANAGER), authHeaderFor(USER_MANAGER1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("a user manager can't change admins' role") {
      withUsers {
        postAsJson(s"/users/$ADMIN1", ChangeUserData.create(Role.USER_MANAGER), authHeaderFor(USER_MANAGER1)) {
          status should equal(403)
          parse(body).extract[Result[_]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.CANT_CHANGE_ROLE))))
          )
        }
      }
    }

    scenario("a user manager can't change admins' password") {
      withUsers {
        postAsJson(s"/users/$ADMIN1", ChangeUserData.create("newpassword", "newpassword"), authHeaderFor(USER_MANAGER1)) {
          status should equal(403)
          parse(body).extract[Result[_]] should have(
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
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword", Role.USER_MANAGER), authHeaderFor(ADMIN1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
        postAsJson(s"/users/$USER_MANAGER1", ChangeUserData.create("newpassword", "newpassword", Role.ADMIN), authHeaderFor(ADMIN1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("invalid new password") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("abc", "abc"), authHeaderFor(ADMIN1)) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD)))
        }
      }
    }

    scenario("wrong confirmation") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "jkjkjkjjkjkjk"), authHeaderFor(ADMIN1)) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
        }
      }
    }

    scenario("user not authenticated") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "jkjkjkjjkjkjk"))(checkNotAuthenticatedError)
      }
    }

    scenario("bad schema") {
      withUsers {
        for (badInput <- List(ChangeUserData(None, None, None),
          ChangeUserData(Some("password"), None, None),
          ChangeUserData(None, Some("confirmation"), None),
          Map("role" -> "NEW_ROLE"))) {
          postAsJson(s"/users/$NORMAL1", badInput, authHeaderFor(ADMIN1)) {
            status should equal(400)
            val result = parse(body).extract[Result[_]]
            result.success should be(false)
            result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
          }
        }
      }
    }

    scenario("email not found") {
      withUsers {
        postAsJson(s"/users/idontexist@bla.com", ChangeUserData.create("newpassword", "newpassword"), authHeaderFor(ADMIN1)) {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have(
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
          delete(s"/users/$user", headers = authHeaderFor(user)) {
            status should be(403)
            parse(body).extract[Result[UserView]] should have(
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
          delete(s"/users/$user", headers = authHeaderFor(NORMAL1)) {
            status should be(403)
            parse(body).extract[Result[UserView]] should have(
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
        delete(s"/users/$NORMAL1", headers = authHeaderFor(USER_MANAGER1)) {
          status should be(200)
          parse(body).extract[Result[UserView]] should have(
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
          delete(s"/users/$user", headers = authHeaderFor(ADMIN1)) {
            status should be(200)
            parse(body).extract[Result[UserView]] should have(
              'success (true)
            )
          }
        }
      }
    }

    scenario("user not authenticated") {
      delete(s"/users/$NORMAL1")(checkNotAuthenticatedError)
    }

    scenario("try to delete user that doesn't exist") {
      withUsers {
        delete("/users/idontexist@users.com", headers = authHeaderFor(ADMIN1)) {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }

  }

}
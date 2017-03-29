package nom.bruno.travelplanner.functional

import nom.bruno.travelplanner.Tables
import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.controllers._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UsersControllerTests extends BaseTravelPlannerStackTest {
  val ADMIN1 = "admin1@users.com"
  val ADMIN2 = "admin2@users.com"
  val USER_MANAGER1 = "usermanager1@users.com"
  val USER_MANAGER2 = "usermanager2@users.com"
  val NORMAL1 = "normal1@users.com"
  val NORMAL2 = "normal2@users.com"

  private[this] def xSessionId(email: String) = {
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + email takeRight (32)
  }

  private[this] def authHeader(email: String) = {
    Map("Cookie" -> s"X-Session-Id=${xSessionId(email)}")
  }

  def withUsers(testCode: => Any): Unit = {
    def authenticatedUser(email: String, role: Role) = {
      for {
        id <- (Tables.users returning Tables.users.map(_.id)) += Tables.User.withSaltedPassword(email, "password", role = role)
        _ <- Tables.sessions += Tables.Session(xSessionId(email), id)
      } yield ()
    }

    def nonAuthenticatedUser(email: String, role: Role) = {
      Tables.users += Tables.User.withSaltedPassword(email, "password", role = role)
    }

    val setupActions = DBIO.seq(
      authenticatedUser(ADMIN1, Role.ADMIN),
      nonAuthenticatedUser(ADMIN2, Role.ADMIN),
      authenticatedUser(USER_MANAGER1, Role.USER_MANAGER),
      nonAuthenticatedUser(USER_MANAGER2, Role.USER_MANAGER),
      authenticatedUser(NORMAL1, Role.NORMAL),
      nonAuthenticatedUser(NORMAL2, Role.NORMAL)
    )
    Await.result(db.run(setupActions), Duration.Inf)
    try {
      testCode
    }
    finally {
      val tearDownActions = DBIO.seq((Tables.users.delete))
      Await.result(db.run(tearDownActions), Duration.Inf)
    }
  }

  feature("add users") {
    scenario("all ok") {
      putAsJson("/users/brunore@email.com", NewUserData("apassword", "apassword")) {
        status should equal(200)
        parse(body).extract[Result[_]].success should be(true)
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
      putAsJson("/users/brunore@email.com", NewUserData("apassword", "apassword")) {
        status should equal(200)
        parse(body).extract[Result[_]].success should be(true)
      }
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
        get("/users", headers = authHeader(ADMIN1)) {
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
        get("/users", headers = authHeader(USER_MANAGER1)) {
          status should equal(200)

          parse(body).extract[Result[List[UserView]]].data should be(Some(List(
            UserView(USER_MANAGER1, Role.USER_MANAGER),
            UserView(USER_MANAGER2, Role.USER_MANAGER),
            UserView(NORMAL1, Role.NORMAL),
            UserView(NORMAL2, Role.NORMAL)
          ).sortBy(_.email)))
        }
        get("/users", headers = authHeader(NORMAL1)) {
          status should equal(200)

          parse(body).extract[Result[List[UserView]]].data should be(Some(List(
            UserView(NORMAL1, Role.NORMAL)
          )))
        }
      }
    }

    scenario("user not authenticated") {
      get("/users") {
        status should equal(401)
        val result = parse(body).extract[Result[_]]
        result.errors.get should be(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
      }
    }
  }

  feature("get one user by email") {
    scenario("user not authenticated") {
      get(s"/users/$NORMAL1") {
        status should equal(401)
        parse(body).extract[Result[UserView]] should have(
          'success (false),
          'errors (Some(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED))))
        )
      }
    }

    scenario("get own user") {
      withUsers {
        get(s"/users/$NORMAL1", headers = authHeader(NORMAL1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }
        get(s"/users/$USER_MANAGER1", headers = authHeader(USER_MANAGER1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(USER_MANAGER1, Role.USER_MANAGER)))
          )
        }
        get(s"/users/$ADMIN1", headers = authHeader(ADMIN1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(ADMIN1, Role.ADMIN)))
          )
        }
      }
    }

    scenario("get other user") {
      // for more detailed rules, please see nom.bruno.travelplanner.unit.UserTests#can see
      withUsers {
        get(s"/users/$ADMIN1", headers = authHeader(NORMAL1)) {
          status should equal(403)

          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
        get(s"/users/$NORMAL1", headers = authHeader(USER_MANAGER1)) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL1, Role.NORMAL)))
          )
        }
        get(s"/users/$ADMIN1", headers = authHeader(USER_MANAGER1)) {
          status should equal(403)

          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
        get(s"/users/$USER_MANAGER1", headers = authHeader(ADMIN1)) {
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
        get("/users/idontexist@bla.com", headers = authHeader(ADMIN1)) {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }
  }

  feature("Change user data") {
    // for more detailed permission rules, please see nom.bruno.travelplanner.unit.UserTests#change role or password
    scenario("a user can change its own password") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword"), authHeader(NORMAL1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("a user can't change its own role") {
      withUsers {
        postAsJson(s"/users/$USER_MANAGER1", ChangeUserData.create(Role.NORMAL), authHeader(USER_MANAGER1)) {
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
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword", Role.USER_MANAGER), authHeader(USER_MANAGER1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("a user manager can't change admins' role") {
      withUsers {
        postAsJson(s"/users/$ADMIN1", ChangeUserData.create(Role.USER_MANAGER), authHeader(USER_MANAGER1)) {
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
        postAsJson(s"/users/$ADMIN1", ChangeUserData.create("newpassword", "newpassword"), authHeader(USER_MANAGER1)) {
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
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "newpassword", Role.USER_MANAGER), authHeader(ADMIN1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
        postAsJson(s"/users/$USER_MANAGER1", ChangeUserData.create("newpassword", "newpassword", Role.ADMIN), authHeader(ADMIN1)) {
          status should equal(200)
          parse(body).extract[Result[_]] should have(
            'success (true)
          )
        }
      }
    }

    scenario("invalid new password") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("abc", "abc"), authHeader(ADMIN1)) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD)))
        }
      }
    }

    scenario("wrong confirmation") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "jkjkjkjjkjkjk"), authHeader(ADMIN1)) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
        }
      }
    }

    scenario("user not authenticated") {
      withUsers {
        postAsJson(s"/users/$NORMAL1", ChangeUserData.create("newpassword", "jkjkjkjjkjkjk")) {
          status should equal(401)
          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED))))
          )
        }
      }
    }

    scenario("bad schema") {
      withUsers {
        for (badInput <- List(ChangeUserData(None, None, None),
          ChangeUserData(Some("password"), None, None),
          ChangeUserData(None, Some("confirmation"), None),
          Map("role" -> "NEW_ROLE"))) {
          postAsJson(s"/users/$NORMAL1", badInput, authHeader(ADMIN1)) {
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
        postAsJson(s"/users/idontexist@bla.com", ChangeUserData.create("newpassword", "newpassword"), authHeader(ADMIN1)) {
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
          delete(s"/users/$user", headers = authHeader(user)) {
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
          delete(s"/users/$user", headers = authHeader(NORMAL1)) {
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
        delete(s"/users/$NORMAL1", headers = authHeader(USER_MANAGER1)) {
          status should be(200)
          parse(body).extract[Result[UserView]] should have(
            'success (true)
          )
        }

        get(s"/users/$NORMAL1", headers = authHeader(USER_MANAGER1)) {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }

    scenario("an admin can delete normal and user manager users") {
      withUsers {
        for (user <- Seq(NORMAL1, USER_MANAGER1)) {
          delete(s"/users/$user", headers = authHeader(ADMIN1)) {
            status should be(200)
            parse(body).extract[Result[UserView]] should have(
              'success (true)
            )
          }

          get(s"/users/$user", headers = authHeader(ADMIN1)) {
            status should equal(404)
            parse(body).extract[Result[UserView]] should have(
              'success (false),
              'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
            )
          }
        }
      }
    }

    scenario("user not authenticated") {
      delete(s"/users/$NORMAL1") {
        status should equal(401)
        parse(body).extract[Result[UserView]] should have(
          'success (false),
          'errors (Some(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED))))
        )
      }
    }

    scenario("try to delete user that doesn't exist") {
      withUsers {
        delete("/users/idontexist@users.com", headers = authHeader(ADMIN1)) {
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
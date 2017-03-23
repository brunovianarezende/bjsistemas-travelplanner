package nom.bruno.travelplanner.functional

import nom.bruno.travelplanner.Tables
import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.servlets._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UsersServletTests extends BaseTravelPlannerServletTest {
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
        Map("passwordConfirmation" -> "!1APassword"))) {
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
}
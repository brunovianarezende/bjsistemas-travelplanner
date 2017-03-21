package nom.bruno.travelplanner

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.servlets._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UsersServletTests extends BaseTravelPlannerServletTest {
  val X_SESSION_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  val AUTH_HEADER = Map("Cookie" -> s"X-Session-Id=$X_SESSION_ID")

  def withUsers(testCode: => Any): Unit = {
    val normalUsers = Tables.users ++= Seq(
      Tables.User.withSaltedPassword("bla@bla.com", "password"),
      Tables.User(None, "ble@bla.com", "passsword", "salt", Role.NORMAL) // no salt will be applied in this user password
    )
    val adminUserAndItsSession = for {
      id <- (Tables.users returning Tables.users.map(_.id)) += Tables.User.withSaltedPassword("admin@admin.com", "password", role = Role.ADMIN)
      _ <- Tables.sessions += Tables.Session(X_SESSION_ID, id)
    } yield ()
    val setupActions = DBIO.seq(
      normalUsers,
      adminUserAndItsSession
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
        get("/users", headers = AUTH_HEADER) {
          status should equal(200)

          parse(body).extract[Result[List[UserView]]].data should be(Some(List(
            UserView("bla@bla.com", Role.NORMAL),
            UserView("ble@bla.com", Role.NORMAL),
            UserView("admin@admin.com", Role.ADMIN)
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
      get("/users/bla@bla.com") {
        status should equal(401)
        parse(body).extract[Result[UserView]] should have(
          'success (false),
          'errors (Some(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED))))
        )
      }
    }

    scenario("email found") {
      withUsers {
        get("/users/bla@bla.com", headers = AUTH_HEADER) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView("bla@bla.com", Role.NORMAL)))
          )
        }
      }
    }

    scenario("email not found") {
      withUsers {
        get("/users/idontexist@bla.com", headers = AUTH_HEADER) {
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
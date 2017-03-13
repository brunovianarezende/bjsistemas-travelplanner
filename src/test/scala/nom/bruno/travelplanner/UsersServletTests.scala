package nom.bruno.travelplanner

import org.json4s._
import org.json4s.jackson.JsonMethods._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UsersServletTests extends BaseTravelPlannerServletTest {
  addServlet(new UsersServlet(db), "/users/*")

  def withUsers(testCode: => Any): Unit = {
    val setupActions = DBIO.seq(
      Tables.users ++= Seq(
        Tables.User(None, "bla@bla.com", "passsword", "salt", "NORMAL"),
        Tables.User(None, "ble@bla.com", "passsword", "salt", "NORMAL")
      )
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
        parse(body).extract[Result[_]].success should be (true)
      }
    }

    scenario("bad schema") {
      for(badInput <- List(Map(),
          List(),
          (1).asInstanceOf[AnyRef],
          Map("password" -> "!1APassword"),
          Map("passwordConfirmation" -> "!1APassword"))) {
        putAsJson("/users/brunore@email.com", badInput) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be (false)
          result.errors.get should be (List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }

    scenario("invalid password") {
      putAsJson("/users/brunore@email.com", NewUserData("abc", "abc")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be (false)
        result.errors.get should be (List(Error(ErrorCodes.INVALID_PASSWORD)))
      }
    }

    scenario("wrong confirmation") {
      putAsJson("/users/brunore@email.com", NewUserData("abcdefg", "1234567")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be (false)
        result.errors.get should be (List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
      }
    }

    scenario("invalid email") {
      putAsJson("/users/brunore", NewUserData("apassword", "apassword")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be (false)
        result.errors.get should be (List(Error(ErrorCodes.INVALID_EMAIL)))
      }
    }

    scenario("user already registered") {
      putAsJson("/users/brunore@email.com", NewUserData("apassword", "apassword")) {
        status should equal(200)
        parse(body).extract[Result[_]].success should be (true)
      }
      putAsJson("/users/brunore@email.com", NewUserData("apassword", "apassword")) {
        status should equal(400)
        val result = parse(body).extract[Result[_]]
        result.success should be (false)
        result.errors.get should be (List(Error(ErrorCodes.USER_ALREADY_REGISTERED)))
      }
    }
  }

  feature("get all users") {
    scenario("no user in db") {
      get("/users") {
        status should equal(200)

        parse(body).extract[Result[List[UserView]]].data should be(Some(List()))
      }
    }

    scenario("some users in db") {
      withUsers {
        get("/users") {
          status should equal(200)

          parse(body).extract[Result[List[UserView]]].data should be(Some(List(
            UserView("bla@bla.com", "NORMAL"),
            UserView("ble@bla.com", "NORMAL")
          )))
        }
      }
    }
  }

  feature("get one user by email") {
    scenario("no user in db") {
      get("/users/bla@bla.com") {
        status should equal(404)
        parse(body).extract[Result[UserView]] should have (
          'success (false),
          'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
        )
      }
    }

    scenario("some users in db - email found") {
      withUsers {
        get("/users/bla@bla.com") {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView("bla@bla.com", "NORMAL")))
          )
        }
      }
    }

    scenario("some users in db - email not found") {
      withUsers {
        get("/users/idontexist@bla.com") {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have (
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }

  }
}
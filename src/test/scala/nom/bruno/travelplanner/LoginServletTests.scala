package nom.bruno.travelplanner

import java.net.HttpCookie

import nom.bruno.travelplanner.servlets._
import org.json4s.jackson.JsonMethods.parse
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LoginServletTests extends BaseTravelPlannerServletTest {
  def withUsers(testCode: => Any): Unit = {
    val setupActions = DBIO.seq(
      Tables.users ++= Seq(
        Tables.User.withSaltedPassword("bla@bla.com", "password"),
        Tables.User(None, "ble@bla.com", "passsword", "salt", "NORMAL") // no salt will be applied in this user password
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


  feature("login") {
    scenario("successful login") {
      withUsers {
        postAsJson("/login", LoginData("bla@bla.com", "password")) {
          status should equal (200)
          val result = parse(body).extract[Result[_]]
          result.success should be(true)
          val cookies = header.get("Set-Cookie")
          cookies should not be (None)
          val httpCookies = HttpCookie.parse(cookies.get)
          httpCookies.size should be (1)
          httpCookies.get(0).getName should be ("X-Session-Id")
        }
      }
    }

    scenario("make sure salt is applied when doing comparison") {
      withUsers {
        postAsJson("/login", LoginData("ble@bla.com", "password")) {
          status should equal(401)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
        }
      }
    }

    scenario("wrong password") {
      withUsers {
        postAsJson("/login", LoginData("bla@bla.com", "wrongpassword")) {
          status should equal(401)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
        }
      }
    }

    scenario("email doesn't exist") {
      postAsJson("/login", LoginData("brunore@email.com", "apassword")) {
        status should equal(401)
        val result = parse(body).extract[Result[_]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_LOGIN)))
      }
    }

    scenario("bad schema") {
      for (badInput <- List(Map(),
        List(),
        (1).asInstanceOf[AnyRef],
        Map("password" -> "!1APassword"),
        Map("email" -> "brunore@email.com"))) {
        postAsJson("/login", badInput) {
          status should equal(400)
          val result = parse(body).extract[Result[_]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }
  }

}

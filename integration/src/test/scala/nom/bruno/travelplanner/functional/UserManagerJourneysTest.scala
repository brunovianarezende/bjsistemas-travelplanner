package nom.bruno.travelplanner.functional

import java.net.HttpCookie

import nom.bruno.travelplanner.Tables
import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.servlets._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.GivenWhenThen
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UserManagerJourneysTest extends BaseTravelPlannerServletTest with GivenWhenThen {
  val NORMAL_USER = "normal@bla.com"
  val USER_MANAGER_USER = "usermanager@bla.com"

  def withUsers(testCode: => Any): Unit = {
    def nonAuthenticatedUser(email: String, role: Role) = {
      Tables.users += Tables.User.withSaltedPassword(email, "password", role = role)
    }

    val setupActions = DBIO.seq(
      nonAuthenticatedUser(USER_MANAGER_USER, Role.USER_MANAGER),
      nonAuthenticatedUser(NORMAL_USER, Role.NORMAL)
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

  private[this] def authHeader(xSessionId: String) = {
    Map("Cookie" -> s"X-Session-Id=$xSessionId")
  }

  feature("user manager journeys") {
    scenario("user manager changes data of a normal user") {
      withUsers {
        Given("a user manager logged in the site")
        val sessionId = postAsJson("/login", LoginData(USER_MANAGER_USER, "password")) {
          status should equal(200)
          val cookies = header.get("Set-Cookie")
          HttpCookie.parse(cookies.get).get(0).getValue
        }

        val headers = authHeader(sessionId)

        Then("he must be able to get info about a normal user")
        get(s"/users/$NORMAL_USER", headers = headers) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL_USER, Role.NORMAL)))
          )
        }

        And("he must also be able to change the role of the normal user")
        postAsJson(s"/users/$NORMAL_USER", ChangeUserData.create(Role.USER_MANAGER), headers = headers) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true)
          )
        }
        get(s"/users/$NORMAL_USER", headers = headers) {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView(NORMAL_USER, Role.USER_MANAGER)))
          )
        }
      }
    }
  }
}
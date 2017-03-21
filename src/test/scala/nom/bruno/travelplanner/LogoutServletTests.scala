package nom.bruno.travelplanner

import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.services.AuthenticationService
import nom.bruno.travelplanner.servlets._
import org.json4s.jackson.JsonMethods.parse
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LogoutServletTests extends BaseTravelPlannerServletTest {
  val X_SESSION_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  val AUTH_HEADER = Map("Cookie" -> s"X-Session-Id=$X_SESSION_ID")

  def withUsers(testCode: => Any): Unit = {
    val adminUserAndItsSession = for {
      id <- (Tables.users returning Tables.users.map(_.id)) += Tables.User.withSaltedPassword("admin@admin.com", "password", role = Role.ADMIN)
      _ <- Tables.sessions += Tables.Session(X_SESSION_ID, id)
    } yield ()
    val setupActions = DBIO.seq(
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


  feature("logout") {
    scenario("successful logout") {
      withUsers {
        post("/logout", headers = AUTH_HEADER) {
          status should equal(200)
          val result = parse(body).extract[Result[_]]
          result.success should be(true)
          val authenticationService = new AuthenticationService(db)
          Await.result(authenticationService.getSessionUser(X_SESSION_ID), Duration.Inf) should be(None)
        }
      }
    }
  }
}

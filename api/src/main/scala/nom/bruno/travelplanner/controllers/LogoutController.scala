package nom.bruno.travelplanner.controllers

import nom.bruno.travelplanner.services.AuthenticationService
import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future

class LogoutController(val db: Database) extends TravelPlannerStack {
  val authService = new AuthenticationService(db)

  post("/logout") {
    new AsyncResult() {
      val is = {
        cookies.get("X-Session-Id") match {
          case Some(sessionId) => authService.deleteSession(sessionId) map { _ =>
            Ok()
          }
          case _ => Future {
            Ok()
          }
        }
      }
    }
  }
}

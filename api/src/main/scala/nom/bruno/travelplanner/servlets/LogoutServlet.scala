package nom.bruno.travelplanner.servlets

import nom.bruno.travelplanner.services.AuthenticationService
import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future

class LogoutServlet(val db: Database) extends TravelPlannerServlet {
  val authService = new AuthenticationService(db)

  post("/") {
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

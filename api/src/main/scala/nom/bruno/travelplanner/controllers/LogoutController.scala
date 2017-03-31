package nom.bruno.travelplanner.controllers

import javax.inject.Inject

import nom.bruno.travelplanner.services.AuthenticationService
import org.scalatra.AsyncResult

import scala.concurrent.Future

class LogoutController @Inject()(val authService: AuthenticationService) extends TravelPlannerStack {
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

package nom.bruno.travelplanner.controllers

import javax.inject.Inject

import nom.bruno.travelplanner.services.UsersService
import org.scalatra.AsyncResult

import scala.concurrent.Future

class LogoutController @Inject()(val usersService: UsersService) extends TravelPlannerStack {
  post("/logout") {
    new AsyncResult() {
      val is = {
        cookies.get("X-Session-Id") match {
          case Some(sessionId) => usersService.finishSession(sessionId) map { _ =>
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

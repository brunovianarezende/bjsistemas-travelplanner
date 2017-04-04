package nom.bruno.travelplanner.controllers

import javax.inject.Inject

import nom.bruno.travelplanner.services.AuthenticationService
import org.scalatra.AsyncResult

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.OptionT
import scalaz.Scalaz._

class LoginController @Inject()(val authService: AuthenticationService) extends TravelPlannerStack {
  post("/login") {
    new AsyncResult() {
      val is = {
        Try(parsedBody.extract[LoginData]) match {
          case Success(loginData) => {
            (for {
              user <- OptionT(authService.authenticateUser(loginData.email, loginData.password))
              sessionId <- authService.createNewSession(user).liftM[OptionT]
            }
              yield {
                cookies.set("X-Session-Id", sessionId)
                Ok()
              }).getOrElse(Unauthorized(Error(ErrorCodes.INVALID_LOGIN)))
          }
          case Failure(_) => Future {
            BadRequest(Error(ErrorCodes.BAD_SCHEMA))
          }
        }
      }
    }
  }
}

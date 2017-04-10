package nom.bruno.travelplanner.controllers

import javax.inject.Inject

import nom.bruno.travelplanner.services.UsersService
import nom.bruno.travelplanner.{Error, ErrorCodes, LoginData}
import org.scalatra.AsyncResult

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.OptionT
import scalaz.Scalaz._

class LoginController @Inject()(val usersService: UsersService) extends TravelPlannerStack {
  post("/login") {
    new AsyncResult() {
      val is = {
        Try(parsedBody.extract[LoginData]) match {
          case Success(loginData) => {
            (for {
              sessionId <- OptionT(usersService.loginUser(loginData.email, loginData.password))
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

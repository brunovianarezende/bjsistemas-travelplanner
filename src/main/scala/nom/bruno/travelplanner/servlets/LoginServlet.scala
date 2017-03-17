package nom.bruno.travelplanner.servlets

import nom.bruno.travelplanner.services.AuthenticationService
import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class LoginData(email: String, password: String)

class LoginServlet(val db: Database) extends TravelPlannerServlet {
  val authService = new AuthenticationService(db)

  post("/") {
    new AsyncResult() {
      val is = {
        Try(parsedBody.extract[LoginData]) match {
          case Success(loginData) => {
            authService.authenticateUser(loginData) flatMap {
              case Some(user) => {
                authService.createNewSession(user) map { sessionId =>
                  cookies.set("X-Session-Id", sessionId)
                  Ok()
                }
              }
              case None => Future {
                Unauthorized(Error(ErrorCodes.INVALID_LOGIN))
              }
            }
          }
          case Failure(_) => Future {
            BadRequest(Error(ErrorCodes.BAD_SCHEMA))
          }
        }
      }
    }
  }
}

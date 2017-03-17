package nom.bruno.travelplanner.servlets

import nom.bruno.travelplanner.Tables.User
import nom.bruno.travelplanner.services.AuthenticationService
import org.scalatra.ScalatraBase
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext

trait AuthenticationSupport extends ScalatraBase {
  protected implicit def executor: ExecutionContext
  def db: Database
  val authService = new AuthenticationService(db)

  // we won't use scentry for the authentication. We'll store our session nonce in the database and since slick needs
  // every thing to be async we can't rely on it (scentry). Also, the `before` filter in scalatra can't handle futures,
  // so we'll need instead to use something that will be called in the route (e.g. `get`) itself. Other problem: I
  // don't want to define fromSession and toSession as scentry requires me to do.
  protected def withLoginRequired(f:(User => Any)) = {
    cookies.get("X-Session-Id") match {
      case Some(sessionId) => {
        authService.getSessionUser(sessionId) map {
          case None => halt(Unauthorized(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
          case Some(user) => f(user)
        }
      }
      case None => halt(Unauthorized(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
    }
  }
}
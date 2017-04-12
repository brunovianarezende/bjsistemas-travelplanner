package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import nom.bruno.travelplanner.service.Directives._
import nom.bruno.travelplanner.services.UsersService
import nom.bruno.travelplanner.{Error, ErrorCodes, LoginData}

import scala.concurrent.ExecutionContext

class LoginRoute @Inject()(val usersService: UsersService)
                          (@Named("EC") implicit val ec: ExecutionContext)
  extends BaseRoutes {
  override def routes: Route = {
    (path("login") & post) {
      entityTP(as[LoginData]) { loginData =>
        onSuccess(usersService.loginUser(loginData.email, loginData.password)) {
          case Some(sessionId) => setCookie(HttpCookie(name = "X-Session-Id", value = sessionId)) {
            complete(Ok)
          }
          case None => reject(TPRejection(StatusCodes.Unauthorized, Error(ErrorCodes.INVALID_LOGIN)))
        }
      }
    }
  }

}

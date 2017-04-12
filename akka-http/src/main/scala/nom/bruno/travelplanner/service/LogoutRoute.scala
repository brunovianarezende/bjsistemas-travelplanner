package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import nom.bruno.travelplanner.services.UsersService

import scala.concurrent.ExecutionContext

class LogoutRoute @Inject()(val usersService: UsersService)
                           (@Named("EC") implicit val ec: ExecutionContext)
  extends BaseRoutes {
  override def routes: Route = {
    (path("logout") & post) {
      optionalCookie("X-Session-Id") {
        case Some(cookiePair) => complete {
          usersService.finishSession(cookiePair.value) map { _ =>
            Ok
          }
        }
        case _ =>
          complete(Ok)
      }
    }
  }

}

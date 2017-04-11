package nom.bruno.travelplanner.service

import javax.inject.Inject

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import nom.bruno.travelplanner._
import nom.bruno.travelplanner.service.Directives._


trait BaseRoutes extends JsonProtocol {

  def rejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case e: TPRejection => complete((e.statusCode, Result[Unit](false, None, Some(e.errors))))
    }
    .result()

  def Ok = Result[Unit](true, None, None)

  def Ok[T](content: T) = Result[T](true, Some(content), None)

  def halt(statusCode: StatusCode, error: Error) = {
    throw HaltException(statusCode, error)
  }

  def routes: Route
}

class AllRoutes @Inject()(
                           userRoutes: UserRoutes,
                           logoutRoute: LogoutRoute
                         ) extends BaseRoutes {
  def routes = handleRejections(rejectionHandler) {
    userRoutes.routes ~ logoutRoute.routes
  }
}

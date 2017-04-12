package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import nom.bruno.travelplanner.{Error, ErrorCodes, TripView}
import nom.bruno.travelplanner.service.Directives._
import nom.bruno.travelplanner.services.{TripsService, UsersService}

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}
import scalaz.OptionT
import scalaz.std.scalaFuture._

class TripRoutes @Inject()(val tripsService: TripsService)(@Named("EC") implicit val ec: ExecutionContext, implicit val usersService: UsersService)
  extends BaseRoutes {
  override def routes: Route = {
    (path("users" / Segment / "trips") & authenticate) { (email, authUser) =>
      get {
        complete(Ok("hi"))
      }
    } ~
      (path("users" / Segment / "trips" / Segment) & authenticate) { (email, tripIdStr, authUser) =>
        get {
          Try(tripIdStr.toInt) match {
            case Success(tripId) => {
              completeTP {
                (for {
                  user <- OptionT(usersService.getUser(email)).filter(authUser.canSeeTripsFrom(_))
                  trip <- OptionT(tripsService.getUserTrip(user, tripId))
                }
                  yield {
                    Ok(TripView.from(trip))
                  }).getOrElse(halt(StatusCodes.NotFound, Error(ErrorCodes.INVALID_TRIP)))
              }
            }
            case _ => reject(TPRejection(StatusCodes.NotFound, Error(ErrorCodes.INVALID_TRIP)))

          }
        } ~
          put {
            complete(Ok("hi"))
          } ~
          post {
            complete(Ok("hi"))
          } ~
          delete {
            complete(Ok("hi"))
          }
      }
  }
}
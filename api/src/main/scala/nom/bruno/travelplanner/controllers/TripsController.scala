package nom.bruno.travelplanner.controllers

import java.time.LocalDate
import javax.inject.Inject

import nom.bruno.travelplanner.Tables.Trip
import nom.bruno.travelplanner.services.{AuthenticationService, TripsService, UsersService}
import org.scalatra.AsyncResult

import scala.util.{Success, Try}
import scalaz.OptionT
import scalaz.Scalaz._

case class TripView(id: String, destination: String, start_date: LocalDate, end_date: LocalDate, comment: String)

object TripView {
  def from(trip: Trip): TripView = {
    TripView(trip.id.get.toString, trip.destination, trip.startDate, trip.endDate, trip.comment)
  }
}

class TripsController @Inject()(val tripsService: TripsService, val usersService: UsersService, val authService: AuthenticationService)
  extends TravelPlannerStack with AuthenticationSupport {

  get("/users/:email/trips") {
    withLoginRequired { _ =>
      Ok("ok!")
    }
  }

  get("/users/:email/trips/:tripId") {
    new AsyncResult {
      val is = {
        withLoginRequired { authUser =>
          val email = params("email")
          val tripIdStr = params("tripId")
          Try(tripIdStr.toInt) match {
            case Success(tripId) => {
              (for {
                user <- OptionT(usersService.getUser(email)) if authUser.canSeeTripsFrom(user)
                trip <- OptionT(tripsService.getUserTrip(user, tripId))
              }
                yield {
                  Ok(TripView.from(trip))
                }).getOrElse(NotFound(Error(ErrorCodes.INVALID_TRIP)))
            }
            case _ => NotFound(Error(ErrorCodes.INVALID_TRIP))
          }
        }
      }
    }
  }

  post("/users/:email/trips") {
    withLoginRequired {
      _ =>
        Ok("ok!")
    }
  }

  put("/users/:email/trips/:tripId") {
    withLoginRequired {
      _ =>
        Ok("ok!")
    }
  }

  delete("/users/:email/trips/:tripId") {
    withLoginRequired {
      _ =>
        Ok("ok!")
    }
  }
}

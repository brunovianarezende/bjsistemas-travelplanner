package nom.bruno.travelplanner.controllers

import java.time.LocalDate

import nom.bruno.travelplanner.Tables.Trip
import nom.bruno.travelplanner.services.{TripsService, UsersService}
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TripsControllerTests extends BaseTravelPlannerStackTest {
  def addTrip(email: String, trip: Trip): Int = {
    val usersService = new UsersService(db)
    val userOpt = Await.result(usersService.getUser(email), Duration.Inf)
    val tripsService = new TripsService(db)
    Await.result(tripsService.addTrip(trip.copy(userId = userOpt.flatMap(_.id).get)), Duration.Inf)
  }

  feature("get a single user trip") {
    scenario("get trip correctly") {
      withUsers {
        val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          -1)
        val id = addTrip(NORMAL1, trip)

        get(s"/users/$NORMAL1/trips/$id", headers = authHeaderFor(NORMAL1)) {
          status should be(200)
          val resultAsMap = parse(body).extract[Result[Map[String, _]]]
          // test we return the right fields if we change TripView
          resultAsMap.data.get should be(Map(
            "id" -> id.toString,
            "destination" -> trip.destination,
            "start_date" -> trip.startDate.toString,
            "end_date" -> trip.endDate.toString,
            "comment" -> trip.comment
          ))
          val result = parse(body).extract[Result[TripView]]
          result.success should be(true)
          result.data.get should be(TripView.from(trip.copy(id = Some(id))))
        }
      }
    }

    scenario("user not authenticated") {
      get("/users/any@user.com/trips/1")(checkNotAuthenticatedError)
    }

    scenario("normal user can't get trip from other user") {
      withUsers {
        val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          -1)
        val id = addTrip(NORMAL2, trip)

        get(s"/users/$NORMAL2/trips/$id", headers = authHeaderFor(NORMAL1)) {
          status should be(404)
          parse(body).extract[Result[Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
        }
      }
    }

    scenario("user manager can't get trip from other user") {
      withUsers {
        val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          -1)
        val id = addTrip(NORMAL2, trip)

        get(s"/users/$NORMAL2/trips/$id", headers = authHeaderFor(USER_MANAGER1)) {
          status should be(404)
          parse(body).extract[Result[Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
        }
      }
    }

    scenario("admin user get trip from other user") {
      withUsers {
        val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          -1)
        val id = addTrip(NORMAL1, trip)

        get(s"/users/$NORMAL1/trips/$id", headers = authHeaderFor(ADMIN1)) {
          status should be(200)
          val result = parse(body).extract[Result[TripView]]
          result.success should be(true)
          result.data.get should be(TripView.from(trip.copy(id = Some(id))))
        }
      }
    }

    scenario("trip nor user exist") {
      withUsers {
        get(s"/users/$NORMAL1/trips/1", headers = authHeaderFor(ADMIN1)) {
          status should be(404)
          parse(body).extract[Result[Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
        }
        get(s"/users/bla/trips/1", headers = authHeaderFor(ADMIN1)) {
          status should be(404)
          parse(body).extract[Result[Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
        }
      }
    }

    scenario("invalid trip id type") {
      withUsers {
        get(s"/users/$NORMAL1/trips/abcdef", headers = authHeaderFor(NORMAL1)) {
          status should be(404)
          parse(body).extract[Result[Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
        }
      }
    }
  }

  feature("add trip") {
    scenario("add trip correctly")(pending)
    scenario("user not authenticated") {
      post("/users/any@user.com/trips", "anything")(checkNotAuthenticatedError)
    }
    scenario("normal user can't add trips to other user")(pending)
    scenario("user manager can't add trips to other user")(pending)
    scenario("admin can add trips to other user")(pending)
    scenario("corner case - trying to add a trip to a non-existent user fails with forbidden")(pending)
  }

  feature("change trip") {
    scenario("change trip correctly")(pending)
    scenario("user not authenticated") {
      put("/users/any@user.com/trips/1")(checkNotAuthenticatedError)
    }
    scenario("normal user can't change trips from other user")(pending)
    scenario("user manager can't change trips from other user")(pending)
    scenario("admin can change trips from other user")(pending)
    scenario("corner case - trying to change a trip from a non-existent user fails with forbidden")(pending)
  }

  feature("delete a trip") {
    scenario("delete trip correctly")(pending)
    scenario("user not authenticated") {
      delete("/users/any@user.com/trips/1")(checkNotAuthenticatedError)
    }
    scenario("normal user can't delete trips from other user")(pending)
    scenario("user manager can't delete trips from other user")(pending)
    scenario("admin can delete trips from other user")(pending)
    scenario("corner case - trying to delete a trip from a non-existent user fails with forbidden")(pending)
  }
}

class TripsControllerSearchTests extends BaseTravelPlannerStackTest {
  feature("search trip") {
    scenario("user doesn't exist")(pending)

    scenario("user not authenticated") {
      get("/users/any@user.com/trips")(checkNotAuthenticatedError)
    }

    scenario("normal user can't get trips from other user")(pending)

    scenario("user manager can't get trips from other user")(pending)

    scenario("get all")(pending)

    scenario("get all - user manager")(pending)

    scenario("get all - admin")(pending)

    scenario("filter by email")(pending)

    scenario("filter by email - admin")(pending)

    scenario("filter by id")(pending)

    scenario("filter by destination")(pending)

    scenario("filter by start date")(pending)

    scenario("filter by start date greater than")(pending)

    scenario("filter by start date less than")(pending)

    scenario("filter by end date")(pending)

    scenario("filter by end date greater than")(pending)

    scenario("filter by end date less than")(pending)

    scenario("filter mixed")(pending)
  }
}

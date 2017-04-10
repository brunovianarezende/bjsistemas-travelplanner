package nom.bruno.travelplanner.controllers

import java.time.LocalDate

import nom.bruno.travelplanner
import nom.bruno.travelplanner.Tables.Trip
import nom.bruno.travelplanner.{Error, ErrorCodes, Result, TripView}
import org.json4s.jackson.JsonMethods.parse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TripsControllerTests extends BaseApiTravelPlannerStackTest {

  feature("get a single user trip") {
    scenario("get trip correctly") {
      withUsers {
        val id = 1
        val trip = Trip(Some(id), "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          u(NORMAL1).id.get)

        when(tripsService.getUserTrip(u(NORMAL1), id)).thenReturn(Future {
          Some(trip)
        })

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
      when(usersService.getSessionUser(any())).thenReturn(Future {
        None
      })
      get("/users/any@user.com/trips/1")(checkNotAuthenticatedError)
    }

    scenario("normal user can't get trip from other user") {
      withUsers {
        val id = 1
        val trip = Trip(Some(id), "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          u(NORMAL2).id.get)
        when(tripsService.getUserTrip(u(NORMAL2), id)).thenReturn(Future {
          Some(trip)
        })

        get(s"/users/$NORMAL2/trips/$id", headers = authHeaderFor(NORMAL1)) {
          status should be(404)
          parse(body).extract[Result[travelplanner.Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
          verify(tripsService, never()).getUserTrip(any(), any())
        }
      }
    }

    scenario("user manager can't get trip from other user") {
      withUsers {
        val id = 1
        val trip = Trip(Some(id), "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          u(NORMAL2).id.get)
        when(tripsService.getUserTrip(u(NORMAL2), id)).thenReturn(Future {
          Some(trip)
        })

        get(s"/users/$NORMAL2/trips/$id", headers = authHeaderFor(USER_MANAGER1)) {
          status should be(404)
          parse(body).extract[Result[Error]] should have(
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_TRIP))))
          )
          verify(tripsService, never()).getUserTrip(any(), any())
        }
      }
    }

    scenario("admin user get trip from other user") {
      withUsers {
        val id = 1
        val trip = Trip(Some(id), "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
          u(NORMAL2).id.get)
        when(tripsService.getUserTrip(u(NORMAL1), id)).thenReturn(Future {
          Some(trip)
        })

        get(s"/users/$NORMAL1/trips/$id", headers = authHeaderFor(ADMIN1)) {
          status should be(200)
          val result = parse(body).extract[Result[TripView]]
          result.success should be(true)
          result.data.get should be(TripView.from(trip.copy(id = Some(id))))
          verify(tripsService, times(1)).getUserTrip(u(NORMAL1), id)
        }
      }
    }

    scenario("trip nor user exist") {
      withUsers {
        val id = 1
        when(tripsService.getUserTrip(u(NORMAL1), id)).thenReturn(Future {
          None
        })

        get(s"/users/$NORMAL1/trips/$id", headers = authHeaderFor(ADMIN1)) {
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

class TripsControllerSearchTests extends BaseApiTravelPlannerStackTest {
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

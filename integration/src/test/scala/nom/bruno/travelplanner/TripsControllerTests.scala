package nom.bruno.travelplanner

class TripsControllerTests extends BaseTravelPlannerStackTest {
  feature("get a single user trip") {
    scenario("get trip correctly")(pending)
    scenario("user not authenticated")(pending)
    scenario("normal user can't get trip from other user")(pending)
    scenario("user manager can't get trip from other user")(pending)
    scenario("admin user get trip from other user")(pending)
  }

  feature("get all user's trip") {
    scenario("get trips correctly")(pending)
    scenario("user not authenticated")(pending)
    scenario("normal user can't get trips from other user")(pending)
    scenario("user manager can't get trips from other user")(pending)
    scenario("admin user get trips from other user")(pending)
  }

  feature("add trip") {
    scenario("add trip correctly")(pending)
    scenario("user not authenticated")(pending)
    scenario("normal user can't add trips to other user")(pending)
    scenario("user manager can't add trips to other user")(pending)
    scenario("admin can add trips to other user")(pending)
    scenario("corner case - trying to add a trip to a non-existent user fails with forbidden")(pending)
  }

  feature("change trip") {
    scenario("change trip correctly")(pending)
    scenario("user not authenticated")(pending)
    scenario("normal user can't change trips from other user")(pending)
    scenario("user manager can't change trips from other user")(pending)
    scenario("admin can change trips from other user")(pending)
    scenario("corner case - trying to change a trip from a non-existent user fails with forbidden")(pending)
  }

  feature("delete a trip") {
    scenario("delete trip correctly")(pending)
    scenario("user not authenticated")(pending)
    scenario("normal user can't delete trips from other user")(pending)
    scenario("user manager can't delete trips from other user")(pending)
    scenario("admin can delete trips from other user")(pending)
    scenario("corner case - trying to delete a trip from a non-existent user fails with forbidden")(pending)
  }
}

class TripsControllerSearchTests extends BaseTravelPlannerStackTest {
  feature("search trip") {
    scenario("user doesn't exist")(pending)

    scenario("user not authenticated")(pending)

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

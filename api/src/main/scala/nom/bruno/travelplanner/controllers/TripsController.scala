package nom.bruno.travelplanner.controllers

import slick.jdbc.JdbcBackend.Database

class TripsController(val db: Database) extends TravelPlannerStack with AuthenticationSupport {
  get("/users/:email/trips") {
    Ok("ok!")
  }
}

package nom.bruno.travelplanner.controllers

import slick.jdbc.JdbcBackend.Database

class TripsController(val db: Database) extends TravelPlannerStack with AuthenticationSupport {
  get("/users/:email/trips") {
    withLoginRequired {_ =>
      Ok("ok!")
    }
  }

  get("/users/:email/trips/:tripId") {
    withLoginRequired {_ =>
      Ok("ok!")
    }
  }

  post("/users/:email/trips") {
    withLoginRequired {_ =>
      Ok("ok!")
    }
  }

  put("/users/:email/trips/:tripId") {
    withLoginRequired {_ =>
      Ok("ok!")
    }
  }

  delete("/users/:email/trips/:tripId") {
    withLoginRequired {_ =>
      Ok("ok!")
    }
  }
}

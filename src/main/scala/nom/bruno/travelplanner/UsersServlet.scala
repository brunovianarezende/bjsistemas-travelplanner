package nom.bruno.travelplanner

import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

class UsersServlet(val db: Database) extends TravelPlannerServlet {
  get("/") {
    new AsyncResult {
      val is = {
        db.run(Tables.users.result) map (users => {
          users.map(user => Map("email" -> user.email, "role" -> user.role))
        })
      }
    }
  }

  get("/:email") {
    new AsyncResult {
      val is = {
        db.run(Tables.users.filter(_.email === params("email")).result) map (users => {
          users.map(user => Map("email" -> user.email, "role" -> user.role))
        })
      }
    }
  }

}

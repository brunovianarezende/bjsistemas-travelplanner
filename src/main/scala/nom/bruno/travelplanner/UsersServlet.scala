package nom.bruno.travelplanner

import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

class UsersServlet(val db: Database) extends TravelPlannerServlet {
  get("/") {
    new AsyncResult {
      val is = {
        val users = TableQuery[Tables.Users]
        db.run(users.result) map (users => {
          users.map(user => Map("email" -> user.email, "role" -> user.role))
        })
      }
    }
  }

}

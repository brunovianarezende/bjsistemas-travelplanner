package nom.bruno.travelplanner

import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._

class TravelPlannerServlet(val db: Database) extends TravelPlannerStack {
  get("/") {
    new AsyncResult {
      val is = {
        val users = TableQuery[Tables.Users]
        db.run(users.length.result)
          .map(total => Map("numUsers" -> total))
      }
    }
  }

}

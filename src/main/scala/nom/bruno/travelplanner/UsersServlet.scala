package nom.bruno.travelplanner

import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

case class UserView(email: String, role: String)

class UsersServlet(val db: Database) extends TravelPlannerServlet {
  get("/") {
    new AsyncResult {
      val is = {
        db.run(Tables.users.result) map (users => {
          Ok(users.map(user => UserView(user.email, user.role)))
        })
      }
    }
  }

  get("/:email") {
    new AsyncResult {
      val is = {
        db.run(Tables.users.filter(_.email === params("email")).result) map (users => {
          users.map(user => UserView(user.email, user.role))
            .headOption match {
              case Some(user) => Ok(user)
              case None => NotFound(Error(ErrorCodes.INVALID_USER))
            }
        })
      }
    }
  }

}

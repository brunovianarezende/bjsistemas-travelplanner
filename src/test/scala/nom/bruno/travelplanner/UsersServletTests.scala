package nom.bruno.travelplanner

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UsersServletTests extends BaseTravelPlannerServletTest {
  addServlet(new UsersServlet(db), "/users/*")

  def withUsers(testCode: => Any): Unit = {
    val insertActions = DBIO.seq(
      Tables.users ++= Seq(
        Tables.User(None, "bla@bla.com", "passsword", "salt", "NORMAL"),
        Tables.User(None, "ble@bla.com", "passsword", "salt", "NORMAL")
      )
    )
    Await.result(db.run(insertActions), Duration.Inf)
    try {
      testCode
    }
    finally {
      val deleteActions = DBIO.seq((Tables.users.delete))
      Await.result(db.run(deleteActions), Duration.Inf)
    }
  }

  feature("get all users") {
    scenario("no user in db") {
      get("/users") {
        status should equal(200)

        parse(body).extract[List[_]] should be(List())
      }
    }

    scenario("some users in db") {
      withUsers {
        get("/users") {
          status should equal(200)

          parse(body).extract[List[_]] should be(List(
            Map("email" -> "bla@bla.com", "role" -> "NORMAL"),
            Map("email" -> "ble@bla.com", "role" -> "NORMAL")
          ))
        }
      }
    }
  }
}
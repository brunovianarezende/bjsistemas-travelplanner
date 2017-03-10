package nom.bruno.travelplanner

import org.json4s._
import org.json4s.jackson.JsonMethods._
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

        parse(body).extract[Result[List[UserView]]].data should be(Some(List()))
      }
    }

    scenario("some users in db") {
      withUsers {
        get("/users") {
          status should equal(200)

          parse(body).extract[Result[List[UserView]]].data should be(Some(List(
            UserView("bla@bla.com", "NORMAL"),
            UserView("ble@bla.com", "NORMAL")
          )))
        }
      }
    }
  }

  feature("get one user by email") {
    scenario("no user in db") {
      get("/users/bla@bla.com") {
        status should equal(404)
        parse(body).extract[Result[UserView]] should have (
          'success (false),
          'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
        )
      }
    }

    scenario("some users in db - email found") {
      withUsers {
        get("/users/bla@bla.com") {
          status should equal(200)

          parse(body).extract[Result[UserView]] should have(
            'success (true),
            'data (Some(UserView("bla@bla.com", "NORMAL")))
          )
        }
      }
    }

    scenario("some users in db - email not found") {
      withUsers {
        get("/users/idontexist@bla.com") {
          status should equal(404)
          parse(body).extract[Result[UserView]] should have (
            'success (false),
            'errors (Some(List(Error(ErrorCodes.INVALID_USER))))
          )
        }
      }
    }

  }
}
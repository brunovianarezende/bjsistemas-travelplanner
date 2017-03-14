package nom.bruno.travelplanner

import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class UsersServlet(val db: Database) extends TravelPlannerServlet {
  val usersService = new UsersService(db)
  get("/") {
    new AsyncResult {
      val is = {
        usersService.getAllUsers map (users => {
          Ok(users.map(user => UserView(user)))
        })
      }
    }
  }

  get("/:email") {
    new AsyncResult {
      val is = {
        usersService.getUser(params("email")) map {
          case Some(user) => Ok(UserView(user))
          case None => NotFound(Error(ErrorCodes.INVALID_USER))
        }
      }
    }
  }

  put("/:email") {
    new AsyncResult {
      val is = {
        val email = params("email")
        Try(parsedBody.extract[NewUserData]) match {
          case Success(newUserData) => {
            for {
              validationResult <- usersService.validateNewUser(email, newUserData)
            } yield {
              validationResult match {
                case Left(error) => BadRequest(error)
                case Right(newUser) => {
                  usersService.addUser(newUser) map (_ => Ok())
                }
              }
            }
          }
          case Failure(e) => Future {
            BadRequest(Error(ErrorCodes.BAD_SCHEMA))
          }
        }
      }
    }
  }

}

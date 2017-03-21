package nom.bruno.travelplanner.servlets

import nom.bruno.travelplanner._
import nom.bruno.travelplanner.services.UsersService
import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import Tables.Role._

case class UserView(email: String, role: Role)

object UserView {
  def apply(user: Tables.User): UserView = {
    UserView(user.email, user.role)
  }
}

case class NewUserData(password: String, password_confirmation: String)

class UsersServlet(val db: Database) extends TravelPlannerServlet with AuthenticationSupport {
  val usersService = new UsersService(db)

  get("/") {
    new AsyncResult {
      val is = {
        withLoginRequired { _ =>
          usersService.getAllUsers map (users => {
            Ok(users.map(user => UserView(user)))
          })
        }
      }
    }
  }

  get("/:email") {
    new AsyncResult {
      val is = {
        withLoginRequired { _ =>
          usersService.getUser(params("email")) map {
            case Some(user) => Ok(UserView(user))
            case None => NotFound(Error(ErrorCodes.INVALID_USER))
          }
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

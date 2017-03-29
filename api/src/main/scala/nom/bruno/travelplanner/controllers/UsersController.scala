package nom.bruno.travelplanner.controllers

import nom.bruno.travelplanner.Tables.Role._
import nom.bruno.travelplanner._
import nom.bruno.travelplanner.services.UsersService
import org.scalatra.AsyncResult
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UserView(email: String, role: Role)

object UserView {
  def apply(user: Tables.User): UserView = {
    UserView(user.email, user.role)
  }
}

case class NewUserData(password: String, password_confirmation: String)

case class ChangeUserData(password: Option[String], password_confirmation: Option[String], role: Option[Role]) {
  def schemaOk(): Boolean = {
    passwordFieldsAreOk && atLeastOneChangeIsDefined
  }

  private[this] def passwordFieldsAreOk = {
    (password.isDefined && password_confirmation.isDefined) ||
      (!password.isDefined && !password_confirmation.isDefined)
  }

  private[this] def atLeastOneChangeIsDefined = {
    (password.isDefined && password_confirmation.isDefined) || role.isDefined
  }

  def isPasswordChange: Boolean = {
    password.isDefined && password_confirmation.isDefined
  }

  def isRoleChange: Boolean = {
    role.isDefined
  }
}

object ChangeUserData {
  def create(password: String, passwordConfirmation: String): ChangeUserData = {
    apply(Some(password), Some(passwordConfirmation), None)
  }

  def create(password: String, passwordConfirmation: String, role: Role): ChangeUserData = {
    apply(Some(password), Some(passwordConfirmation), Some(role))
  }

  def create(role: Role): ChangeUserData = {
    apply(None, None, Some(role))
  }

}

class UsersController(val db: Database) extends TravelPlannerStack with AuthenticationSupport {
  val usersService = new UsersService(db)

  get("/users") {
    new AsyncResult {
      val is = {
        withLoginRequired { authUser =>
          usersService.getUsers(authUser) map (users => {
            Ok(users.map(user => UserView(user)))
          })
        }
      }
    }
  }

  get("/users/:email") {
    new AsyncResult {
      val is = {
        withLoginRequired { authUser =>
          usersService.getUser(params("email")) map {
            case Some(user) if authUser.canSee(user) => Ok(UserView(user))
            case Some(user) => Forbidden(Error(ErrorCodes.INVALID_USER))
            case None => NotFound(Error(ErrorCodes.INVALID_USER))
          }
        }
      }
    }
  }

  put("/users/:email") {
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

  post("/users/:email") {
    new AsyncResult {
      val is = {
        val email = params("email")
        withLoginRequired { authUser =>
          Try(parsedBody.extract[ChangeUserData]) match {
            case Success(changeUserData) => {
              for {
                validationResult <- usersService.validateChangeUser(authUser, email, changeUserData)
              } yield {
                validationResult match {
                  case Left(error) if error.code == ErrorCodes.INVALID_USER => NotFound(error)
                  case Left(error) if Set(ErrorCodes.CANT_CHANGE_PASSWORD, ErrorCodes.CANT_CHANGE_ROLE) contains error.code => Forbidden(error)
                  case Left(error) => BadRequest(error)
                  case Right(userToChange) => {
                    usersService.updateUser(userToChange, changeUserData) map (_ => Ok())
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

  delete("/users/:email") {
    new AsyncResult {
      val is = {
        val email = params("email")
        withLoginRequired { authUser =>
          for {
            validationResult <- usersService.validateDeleteUser(authUser, email)
          } yield {
            validationResult match {
              case Left(error) if error.code == ErrorCodes.INVALID_USER => NotFound(error)
              case Left(error) => Forbidden(error)
              case Right(userToDelete) => {
                usersService.deleteUser(userToDelete) map (_ => Ok())
              }
            }
          }
        }
      }
    }
  }

}

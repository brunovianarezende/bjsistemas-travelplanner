package nom.bruno.travelplanner.controllers

import javax.inject.Inject

import nom.bruno.travelplanner.Tables.Role._
import nom.bruno.travelplanner.Tables.User
import nom.bruno.travelplanner._
import nom.bruno.travelplanner.services.{AuthenticationService, UsersService}
import org.scalatra.AsyncResult

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.OptionT
import scalaz.Scalaz._

class UsersController @Inject()(val usersService: UsersService, val authService: AuthenticationService)
  extends TravelPlannerStack with AuthenticationSupport {
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
              validationResult <- validateNewUser(email, newUserData)
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

  private[this] def validateNewUser(email: String, newUserData: NewUserData): Future[Either[Error, User]] = {
    if (!validateEmail(email)) {
      Future {
        Left(Error(ErrorCodes.INVALID_EMAIL))
      }
    }
    else if (!validatePassword(newUserData.password)) {
      Future {
        Left(Error(ErrorCodes.INVALID_PASSWORD))
      }
    }
    else if (newUserData.password != newUserData.password_confirmation) {
      Future {
        Left(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION))
      }
    }
    else {
      for (userOpt <- usersService.getUser(email)) yield {
        userOpt match {
          case None => Right(User.withSaltedPassword(email, newUserData.password))
          case _ => Left(Error(ErrorCodes.USER_ALREADY_REGISTERED))
        }
      }
    }
  }

  private[this] def validatePassword(password: String) = {
    password.length > 6
  }

  private[this] def validateEmail(email: String) = {
    UsersController.EMAIL_PATTERN.pattern.matcher(email).matches
  }


  post("/users/:email") {
    new AsyncResult {
      val is = {
        val email = params("email")
        withLoginRequired { authUser =>
          Try(parsedBody.extract[ChangeUserData]) match {
            case Success(changeUserData) => {
              for {
                validationResult <- validateChangeUser(authUser, email, changeUserData)
              } yield {
                validationResult match {
                  case Left(error) if error.code == ErrorCodes.INVALID_USER => NotFound(error)
                  case Left(error) if Set(ErrorCodes.CANT_CHANGE_PASSWORD, ErrorCodes.CANT_CHANGE_ROLE) contains error.code => Forbidden(error)
                  case Left(error) => BadRequest(error)
                  case Right(userToChange) => {
                    usersService.updateUser(userToChange, changeUserData.password, changeUserData.role) map (_ => Ok())
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

  def validateChangeUser(authUser: User, email: String, newData: ChangeUserData): Future[Either[Error, User]] = {
    def f = Future

    if (!newData.schemaOk()) {
      f(Left(Error(ErrorCodes.BAD_SCHEMA)))
    }
    else if (newData.isPasswordChange && !validatePassword(newData.password.get)) {
      f(Left(Error(ErrorCodes.INVALID_PASSWORD)))
    }
    else if (newData.isPasswordChange && newData.password != newData.password_confirmation) {
      f(Left(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
    }
    else {
      (for {
        user <- OptionT(usersService.getUser(email))
      } yield {
        if (newData.isPasswordChange && !authUser.canChangePassword(user)) {
          Left(Error(ErrorCodes.CANT_CHANGE_PASSWORD))
        }
        else if (newData.isRoleChange && !authUser.canChangeRole(user, newData.role.get)) {
          Left(Error(ErrorCodes.CANT_CHANGE_ROLE))
        }
        else {
          Right(user)
        }
      }).getOrElse(Left(Error(ErrorCodes.INVALID_USER)))
    }
  }

  delete("/users/:email") {
    new AsyncResult {
      val is = {
        val email = params("email")
        withLoginRequired { authUser =>
          for {
            validationResult <- validateDeleteUser(authUser, email)
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

  def validateDeleteUser(authUser: User, email: String): Future[Either[Error, User]] = {
    (for {
      user <- OptionT(usersService.getUser(email))
    } yield {
      if (authUser.canDelete(user)) {
        Right(user)
      }
      else {
        Left(Error(ErrorCodes.CANT_DELETE_USER))
      }
    }).getOrElse(Left(Error(ErrorCodes.INVALID_USER)))
  }

}

object UsersController {
  private val EMAIL_PATTERN = """^[^@]+@[^@]+$""".r
}
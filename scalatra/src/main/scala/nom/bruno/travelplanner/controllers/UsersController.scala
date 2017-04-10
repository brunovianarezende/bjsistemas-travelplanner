package nom.bruno.travelplanner.controllers

import javax.inject.Inject

import nom.bruno.travelplanner._
import nom.bruno.travelplanner.services.{UsersService, ValidationService}
import org.scalatra.AsyncResult

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class UsersController @Inject()(val usersService: UsersService, val validationService: ValidationService)
  extends TravelPlannerStack with AuthenticationSupport {
  get("/users") {
    new AsyncResult {
      val is = {
        withLoginRequired { authUser =>
          usersService.getUsersVisibleFor(authUser) map (users => {
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
              validationResult <- validationService.validateNewUser(email, newUserData)
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
                validationResult <- validationService.validateChangeUser(authUser, email, changeUserData)
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

  delete("/users/:email") {
    new AsyncResult {
      val is = {
        val email = params("email")
        withLoginRequired { authUser =>
          for {
            validationResult <- validationService.validateDeleteUser(authUser, email)
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

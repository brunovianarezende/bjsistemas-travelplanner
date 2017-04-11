package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import nom.bruno.travelplanner._
import nom.bruno.travelplanner.service.Directives._
import nom.bruno.travelplanner.services.{UsersService, ValidationService}

import scala.concurrent.ExecutionContext


class Routes @Inject()(val usersService: UsersService, val validationService: ValidationService)
                      (@Named("EC") implicit val ec: ExecutionContext)
  extends JsonProtocol {


  private[this] def rejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case e: TPRejection => complete((e.statusCode, Result[Unit](false, None, Some(e.errors))))
    }
    .result()

  def Ok = Result[Unit](true, None, None)

  def Ok[T](content: T) = Result[T](true, Some(content), None)

  private[this] def halt(statusCode: StatusCode, error: Error) = {
    throw HaltException(statusCode, error)
  }

  val routes = handleRejections(rejectionHandler) {
    pathPrefix("users" / """.+""".r) { email =>
      put {
        entityTP(as[NewUserData]) {
          newUserData =>
            completeTP {
              for {
                validationResult <- validationService.validateNewUser(email, newUserData)
              } yield {
                validationResult match {
                  case Left(error) => halt(StatusCodes.BadRequest, error)
                  case Right(newUser) => {
                    usersService.addUser(newUser) map (_ => Ok)
                  }
                }
              }
            }
        }
      } ~
        (get & authenticate) { authUser =>
          completeTP {
            usersService.getUser(email) map {
              case Some(user) if authUser.canSee(user) => Ok(UserView(user))
              case Some(user) => halt(StatusCodes.Forbidden, Error(ErrorCodes.INVALID_USER))
              case None => halt(StatusCodes.NotFound, Error(ErrorCodes.INVALID_USER))
            }
          }
        } ~
        (post & authenticate) { authUser =>
          entityTP(as[ChangeUserData]) { changeUserData =>
            completeTP {
              val forbiddenErrors = Set(ErrorCodes.CANT_CHANGE_PASSWORD, ErrorCodes.CANT_CHANGE_ROLE)
              for {
                validationResult <- validationService.validateChangeUser(authUser, email, changeUserData)
              } yield {
                validationResult match {
                  case Left(error) if error.code == ErrorCodes.INVALID_USER => halt(StatusCodes.NotFound, error)
                  case Left(error) if forbiddenErrors contains error.code => halt(StatusCodes.Forbidden, error)
                  case Left(error) => halt(StatusCodes.BadRequest, error)
                  case Right(userToChange) => {
                    usersService.updateUser(userToChange, changeUserData.password, changeUserData.role) map (_ => Ok())
                  }
                }
              }
            }
          }
        } ~
        (delete & authenticate) { authUser =>
          completeTP {
            for {
              validationResult <- validationService.validateDeleteUser(authUser, email)
            } yield {
              validationResult match {
                case Left(error) if error.code == ErrorCodes.INVALID_USER => halt(StatusCodes.NotFound, error)
                case Left(error) => halt(StatusCodes.Forbidden, error)
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

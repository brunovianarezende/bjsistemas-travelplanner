package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import nom.bruno.travelplanner.Tables.User
import nom.bruno.travelplanner.service.Directives._
import nom.bruno.travelplanner.services.{AuthenticationService, UsersService}
import nom.bruno.travelplanner.{Error, ErrorCodes, NewUserData, Result, UserView}

import scala.concurrent.{ExecutionContext, Future}


class Routes @Inject()(val usersService: UsersService, val authService: AuthenticationService)(@Named("EC") implicit val ec: ExecutionContext)
  extends JsonProtocol {


  private[this] def rejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case e: TPRejection => complete((e.statusCode, Result[Unit](false, None, Some(e.errors))))
    }
    .result()

  def Ok = Result[Unit](true, None, None)

  def Ok[T](content: T) = Result[T](true, Some(content), None)

  val routes = handleRejections(rejectionHandler) {
    pathPrefix("users" / """.+""".r) { email =>
      put {
        entityTP(as[NewUserData]) {
          newUserData =>
            completeTP {
              for {
                validationResult <- validateNewUser(email, newUserData)
              } yield {
                validationResult match {
                  case Left(error) => throw HaltException(StatusCodes.BadRequest, error)
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
              case Some(user) => throw HaltException(StatusCodes.Forbidden, Error(ErrorCodes.INVALID_USER))
              case None => throw HaltException(StatusCodes.NotFound, Error(ErrorCodes.INVALID_USER))
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
    """^[^@]+@[^@]+$""".r.pattern.matcher(email).matches
  }

}

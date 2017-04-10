package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import nom.bruno.travelplanner.service.Directives._
import nom.bruno.travelplanner.services.{UsersService, ValidationService}
import nom.bruno.travelplanner._

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
}

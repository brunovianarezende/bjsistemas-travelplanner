package nom.bruno.travelplanner.service

import javax.inject.{Inject, Named}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{Directive1, _}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import nom.bruno.travelplanner.Tables.{Role, User}
import nom.bruno.travelplanner.services.{AuthenticationService, UsersService}
import nom.bruno.travelplanner.utils.CustomJsonFormats._
import nom.bruno.travelplanner.{ChangeUserData, Error, ErrorCodes, NewUserData, Result, UserView}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


trait JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val newUserDataFormat = jsonFormat2(NewUserData.apply)
  implicit val errorFormat = jsonFormat1(Error.apply)
  implicit val roleFormat = jsonEnum(Role)
  implicit val changeUserDataFormata = jsonFormat3(ChangeUserData.apply)
  implicit val userViewFormat = jsonFormat2(UserView.apply)

  implicit def resultFomat[T: JsonFormat] = jsonFormat3(Result.apply[T])
}

case class TPRejection(statusCode: StatusCode, errors: List[Error]) extends Rejection

object TPRejection {
  def apply(statusCode: StatusCode, error: Error): TPRejection = {
    TPRejection(statusCode, List(error))
  }
}

class HaltException(val statusCode: StatusCode, val errors: List[Error]) extends Exception

object HaltException {
  def apply(statusCode: StatusCode, errors: List[Error]): HaltException = {
    new HaltException(statusCode, errors)
  }

  def apply(statusCode: StatusCode, error: Error): HaltException = {
    HaltException(statusCode, List(error))
  }
}


class Routes @Inject()(val usersService: UsersService, val authService: AuthenticationService)(@Named("EC") implicit val ec: ExecutionContext)
  extends JsonProtocol {

  def entityTP[T](um: FromRequestUnmarshaller[T]): Directive1[T] = {
    entity(um).recover(rejections => reject(TPRejection(StatusCodes.BadRequest, List(Error(ErrorCodes.BAD_SCHEMA)))))
  }

  def authenticate: Directive1[User] = {
    def failure = reject(TPRejection(StatusCodes.Unauthorized, Error(ErrorCodes.USER_NOT_AUTHENTICATED)))

    optionalCookie("X-Session-Id") flatMap {
      case Some(cookiePair) => {
        val sessionId = cookiePair.value
        onSuccess(authService.getSessionUser(sessionId)) flatMap {
          case Some(user) => provide(user)
          case _ => failure
        }

      }
      case _ => failure
    }
  }

  def completeTP[T](future: => Future[T])(implicit m: ToResponseMarshaller[T]): Route = {
    onComplete(future) {
      case Success(res) => complete(res)
      case Failure(exception) => exception match {
        case halt: HaltException => reject(TPRejection(halt.statusCode, halt.errors))
        case _ => reject(TPRejection(StatusCodes.InternalServerError, List(Error(ErrorCodes.INTERNAL_ERROR))))
      }
    }
  }

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

  private[this] def validateNewUser(email: String, newUserData: NewUserData): Future[Either[Error, User]]

  = {
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

  private[this] def validatePassword(password: String)

  = {
    password.length > 6
  }

  private[this] def validateEmail(email: String)

  = {
    """^[^@]+@[^@]+$""".r.pattern.matcher(email).matches
  }

}

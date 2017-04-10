package nom.bruno.travelplanner.service

import javax.inject.Inject

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{Directive1, Rejection, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import nom.bruno.travelplanner.Tables.User
import nom.bruno.travelplanner.services.AuthenticationService
import nom.bruno.travelplanner.{Error, ErrorCodes}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Directives {
  @Inject
  var authService: AuthenticationService = null;

  case class TPRejection(statusCode: StatusCode, errors: List[Error]) extends Rejection

  object TPRejection {
    def apply(statusCode: StatusCode, error: Error): TPRejection = {
      TPRejection(statusCode, List(error))
    }
  }

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

  class HaltException(val statusCode: StatusCode, val errors: List[Error]) extends Exception

  object HaltException {
    def apply(statusCode: StatusCode, errors: List[Error]): HaltException = {
      new HaltException(statusCode, errors)
    }

    def apply(statusCode: StatusCode, error: Error): HaltException = {
      HaltException(statusCode, List(error))
    }
  }

}

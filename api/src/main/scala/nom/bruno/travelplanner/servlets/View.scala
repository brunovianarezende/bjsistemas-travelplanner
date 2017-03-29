package nom.bruno.travelplanner.servlets

import org.{scalatra => s}

object ErrorCodes {
  val MISSING_FIELDS = 1
  val USER_ALREADY_REGISTERED = 2
  val INVALID_EMAIL = 3
  val INVALID_PASSWORD = 4
  val INVALID_PASSWORD_CONFIRMATION = 5
  val INVALID_LOGIN = 6
  val INVALID_USER = 7
  val USER_NOT_AUTHENTICATED = 8
  val INVALID_TRIP = 9
  val INVALID_ROLE_VALUE = 10
  val INVALID_FIELDS = 11
  val BAD_SCHEMA = 12
  val CANT_CHANGE_PASSWORD = 13
  val CANT_CHANGE_ROLE = 14
  val CANT_DELETE_USER = 15
}

case class Error(code: Int)

case class Result[T](success: Boolean, data: Option[T], errors: Option[List[Error]])

object Ok {
  def apply() = {
    s.Ok(Result(true, None, None))
  }

  def apply(data: Any) = {
    s.Ok(Result(true, Some(data), None))
  }
}

object NotFound {
  def apply(error: Error) = {
    s.NotFound(Result(false, None, Some(List(error))))
  }
}

object BadRequest {
  def apply(error: Error) = {
    s.BadRequest(Result(false, None, Some(List(error))))
  }
}

object Unauthorized {
  def apply(error: Error) = {
    s.Unauthorized(Result(false, None, Some(List(error))))
  }
}

object Forbidden {
  def apply(error: Error) = {
    s.Forbidden(Result(false, None, Some(List(error))))
  }
}
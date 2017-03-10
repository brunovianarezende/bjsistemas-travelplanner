package nom.bruno.travelplanner

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
}

case class Error(code: Int)

case class Result[T](success: Boolean, data: Option[T], errors: Option[List[Error]])

object NotFound {
  def apply(error: Error) = {
    s.NotFound(Result(false, None, Some(List(error))))
  }
}

object Ok {
  def apply(data: Any) = {
    s.Ok(Result(true, Some(data), None))
  }
}

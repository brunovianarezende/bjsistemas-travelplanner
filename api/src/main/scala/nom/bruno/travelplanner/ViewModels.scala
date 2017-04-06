package nom.bruno.travelplanner

import java.time.LocalDate

import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.Tables.Trip

case class LoginData(email: String, password: String)

case class TripView(id: String, destination: String, start_date: LocalDate, end_date: LocalDate, comment: String)

object TripView {
  def from(trip: Trip): TripView = {
    TripView(trip.id.get.toString, trip.destination, trip.startDate, trip.endDate, trip.comment)
  }
}

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

object ErrorCodes {
  val INTERNAL_ERROR = 0
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

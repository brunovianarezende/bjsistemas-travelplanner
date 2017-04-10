package nom.bruno.travelplanner.services

import javax.inject.{Inject, Named}

import nom.bruno.travelplanner.Tables.User
import nom.bruno.travelplanner.{ChangeUserData, Error, ErrorCodes, NewUserData}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.OptionT
import scalaz.Scalaz._

class ValidationService @Inject()(usersService: UsersService)
                                 (@Named("EC") implicit val executionContext: ExecutionContext) {
  def validateNewUser(email: String, newUserData: NewUserData): Future[Either[Error, User]] = {
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
    ValidationService.EMAIL_PATTERN.pattern.matcher(email).matches
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

object ValidationService {
  private val EMAIL_PATTERN = """^[^@]+@[^@]+$""".r
}
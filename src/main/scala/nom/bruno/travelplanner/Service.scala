package nom.bruno.travelplanner

import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}
import slick.jdbc.MySQLProfile.api._

class UsersService(val db: Database) {
  def validateNewUser(email: String, newUserData: NewUserData): Future[Either[Error, Tables.User]] = {
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
      db.run(Tables.users.filter(_.email === email).length.result) map (numUsers => {
        if(numUsers == 0) {
          Right(Tables.User.withSaltedPassword(email, newUserData.password))
        }
        else {
          Left(Error(ErrorCodes.USER_ALREADY_REGISTERED))
        }
      })
    }
  }

  private[this] def validatePassword(password: String) = {
    password.length > 6
  }

  private[this] def validateEmail(email: String) = {
    UsersService.EMAIL_PATTERN.pattern.matcher(email).matches
  }
}

object UsersService {
  private val EMAIL_PATTERN = """^[^@]+@[^@]+$""".r
}
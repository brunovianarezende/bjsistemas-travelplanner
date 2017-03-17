package nom.bruno.travelplanner.services

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import nom.bruno.travelplanner.Tables.{User, users}
import nom.bruno.travelplanner.servlets.{Error, ErrorCodes, NewUserData}

import scala.concurrent.{ExecutionContext, Future}

class UsersService(val db: Database)(implicit val executionContext: ExecutionContext) {
  def getAllUsers: Future[Seq[User]] = db.run(users.result)

  def getUser(email: String): Future[Option[User]] = db.run(users.filter(_.email === email).result.headOption)

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
      db.run(users.filter(_.email === email).length.result) map (numUsers => {
        if (numUsers == 0) {
          Right(User.withSaltedPassword(email, newUserData.password))
        }
        else {
          Left(Error(ErrorCodes.USER_ALREADY_REGISTERED))
        }
      })
    }
  }

  def addUser(user: User): Future[Unit] = {
    val insertActions = DBIO.seq(
      users += user
    )
    db.run(insertActions)
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
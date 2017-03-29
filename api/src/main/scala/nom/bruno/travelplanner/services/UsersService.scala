package nom.bruno.travelplanner.services

import nom.bruno.travelplanner.Tables.{Role, User, users}
import nom.bruno.travelplanner.servlets.{ChangeUserData, Error, ErrorCodes, NewUserData}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class UsersService(val db: Database)(implicit val executionContext: ExecutionContext) {
  def getUsers(authUser: User): Future[Seq[User]] = {
    val query = authUser.role match {
      case Role.ADMIN => users.filter(user => user.role inSet List(Role.NORMAL, Role.USER_MANAGER, Role.ADMIN))
      case Role.USER_MANAGER => users.filter(user => user.role inSet List(Role.NORMAL, Role.USER_MANAGER))
      case _ => users.filter(_.id === authUser.id)
    }
    db.run(query.sortBy(_.email).result)
  }

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
      for {
        userOpt <- getUser(email)
      } yield {
        userOpt match {
          case Some(user) => if (newData.isPasswordChange && !authUser.canChangePassword(user)) {
            Left(Error(ErrorCodes.CANT_CHANGE_PASSWORD))
          }
          else if (newData.isRoleChange && !authUser.canChangeRole(user, newData.role.get)) {
            Left(Error(ErrorCodes.CANT_CHANGE_ROLE))
          }
          else {
            Right(user)
          }
          case _ => Left(Error(ErrorCodes.INVALID_USER))
        }
      }
    }
  }

  def validateDeleteUser(authUser: User, email: String): Future[Either[Error, User]] = {
    for {
      userOpt <- getUser(email)
    } yield {
      userOpt match {
        case Some(user) => {
          if (authUser.canDelete(user)) {
            Right(user)
          }
          else {
            Left(Error(ErrorCodes.CANT_DELETE_USER))
          }
        }
        case _ => Left(Error(ErrorCodes.INVALID_USER))
      }
    }
  }


  def addUser(user: User): Future[Unit] = {
    val insertActions = DBIO.seq(
      users += user
    )
    db.run(insertActions)
  }

  def updateUser(user: User, diff: ChangeUserData): Future[Int] = {
    val q = for {
      u <- users if u.id === user.id
    } yield {
      (u.password, u.role)
    }
    val password = if (diff.isPasswordChange) {
      user.encodePassword(diff.password.get)
    }
    else {
      user.password
    }
    val updateAction = q.update((password, diff.role.getOrElse(user.role)))
    db.run(updateAction)
  }

  def deleteUser(user: User): Future[Int] = {
    val q = users.filter(_.id === user.id)
    db.run(q.delete)
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
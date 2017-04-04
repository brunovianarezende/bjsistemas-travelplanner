package nom.bruno.travelplanner.services

import javax.inject.{Inject, Named}

import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.Tables.{Role, User, users}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class UsersService @Inject()(val db: Database)(@Named("EC") implicit val executionContext: ExecutionContext) {
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

  def addUser(user: User): Future[Unit] = {
    val insertActions = DBIO.seq(
      users += user
    )
    db.run(insertActions)
  }

  def updateUser(user: User, optPassword: Option[String], optRole: Option[Role]): Future[Int] = {
    val q = for {
      u <- users if u.id === user.id
    } yield {
      (u.password, u.role)
    }
    val password = optPassword match {
      case Some(s) => user.encodePassword(s)
      case None => user.password
    }
    val updateAction = q.update((password, optRole.getOrElse(user.role)))
    db.run(updateAction)
  }

  def deleteUser(user: User): Future[Int] = {
    val q = users.filter(_.id === user.id)
    db.run(q.delete)
  }
}
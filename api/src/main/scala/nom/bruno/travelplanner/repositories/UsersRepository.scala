package nom.bruno.travelplanner.repositories

import javax.inject.{Inject, Named}

import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.Tables.{Role, User, users}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class UsersRepository @Inject()(val db: Database)(@Named("EC") implicit val executionContext: ExecutionContext) {
  def getUsersByRoles(roles: Seq[Role]): Future[Seq[User]] = {
    val query = users.filter(user => user.role inSet roles)
    db.run(query.sortBy(_.email).result)
  }

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
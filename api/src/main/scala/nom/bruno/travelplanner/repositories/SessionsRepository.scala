package nom.bruno.travelplanner.repositories

import javax.inject.{Inject, Named}

import nom.bruno.travelplanner.Tables.{Session, User, sessions}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SessionsRepository @Inject()(val db: Database)(@Named("EC") implicit val executionContext: ExecutionContext) {
  def createNewSession(user: User): Future[String] = {
    val newSession = Session.createNewForUser(user)
    var insertActions = DBIO.seq(
      sessions += newSession
    )
    db.run(insertActions) map { _ =>
      newSession.sessionId
    }
  }

  def getSessionUser(sessionId: String): Future[Option[User]] = {
    val query = sessions.filter(_.sessionId === sessionId).flatMap(_.user).result.headOption
    db.run(query)
  }

  def deleteSession(sessionId: String): Future[Int] = {
    val action = sessions.filter(_.sessionId === sessionId).delete
    db.run(action)
  }
}

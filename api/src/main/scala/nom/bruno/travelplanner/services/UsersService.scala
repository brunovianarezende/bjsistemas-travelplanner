package nom.bruno.travelplanner.services

import javax.inject.{Inject, Named}

import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.Tables.{Role, User}
import nom.bruno.travelplanner.repositories.{SessionsRepository, UsersRepository}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.OptionT
import scalaz.Scalaz._

class UsersService @Inject()(usersRepository: UsersRepository, sessionsRepository: SessionsRepository)
                            (@Named("EC") implicit val executionContext: ExecutionContext) {
  def loginUser(email: String, password: String): Future[Option[String]] = {
    (for {
      user <- OptionT(authenticateUser(email, password))
      sessionId <- sessionsRepository.createNewSession(user).liftM[OptionT]
    }
      yield {
        sessionId
      }).run
  }

  private[this] def authenticateUser(email: String, password: String): Future[Option[User]] = {
    getUser(email) map {
      case Some(user) if user.checkPassword(password) => Some(user)
      case _ => None
    }
  }

  def getSessionUser(sessionId: String): Future[Option[User]] = {
    sessionsRepository.getSessionUser(sessionId)
  }

  def finishSession(sessionId: String): Future[Unit] = {
    sessionsRepository.deleteSession(sessionId) map { _ => }
  }

  def getUsersVisibleFor(authUser: User): Future[Seq[User]] = {
    authUser.role match {
      case Role.ADMIN => usersRepository.getUsersByRoles(Seq(Role.NORMAL, Role.USER_MANAGER, Role.ADMIN))
      case Role.USER_MANAGER => usersRepository.getUsersByRoles(Seq(Role.NORMAL, Role.USER_MANAGER))
      case _ => usersRepository.getUser(authUser.email) map (_.toSeq)
    }
  }

  def getUser(email: String): Future[Option[User]] = {
    usersRepository.getUser(email)
  }

  def addUser(user: User): Future[Unit] = {
    usersRepository.addUser(user)
  }

  def updateUser(user: User, optPassword: Option[String], optRole: Option[Role]): Future[Int] = {
    usersRepository.updateUser(user, optPassword, optRole)
  }

  def deleteUser(user: User): Future[Int] = {
    usersRepository.deleteUser(user)
  }
}

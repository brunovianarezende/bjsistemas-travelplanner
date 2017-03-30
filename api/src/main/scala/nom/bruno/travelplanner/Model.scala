package nom.bruno.travelplanner

import java.security.MessageDigest
import java.sql.Date
import java.time.LocalDate
import java.util.UUID

import org.apache.commons.codec.binary.Hex
import slick.jdbc.MySQLProfile.api._

object Tables {

  object Role extends Enumeration {
    type Role = Value
    val NORMAL = Value("NORMAL")
    val USER_MANAGER = Value("USER_MANAGER")
    val ADMIN = Value("ADMIN")

    implicit val rolesMapper = MappedColumnType.base[Role, String](
      e => e.toString,
      s => Role.withName(s)
    )
  }

  import Role._

  case class User(id: Option[Int], email: String, password: String, salt: String, role: Role) {
    def encodePassword(s: String): String = {
      User.applySalt(s, this.salt)
    }

    def canSee(other: User): Boolean = {
      this.id == other.id || (this.role match {
        case NORMAL => false
        case _ => this.role >= other.role
      })
    }

    def canSeeTripsFrom(other: User) = {
      this.id == other.id || this.role == ADMIN
    }

    def canChangeRole(other: User, role: Role): Boolean = {
      this.id != other.id && (this.role match {
        case NORMAL => false
        case _ => this.role >= other.role && this.role >= role
      })
    }

    def canChangePassword(other: User): Boolean = {
      this.id == other.id || (this.role match {
        case NORMAL => false
        case _ => this.role >= other.role
      })
    }

    def canDelete(other: User): Boolean = {
      this.id != other.id && this.role > other.role
    }

    def checkPassword(otherPassword: String): Boolean = {
      password == User.applySalt(otherPassword, salt)
    }
  }

  class Users(tag: Tag) extends Table[User](tag, "user") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def email = column[String]("email", O.Unique, O.Length(120))

    def password = column[String]("password", O.Length(40))

    def salt = column[String]("salt", O.Length(32))

    def role = column[Role]("role")

    def * = (id.?, email, password, salt, role) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  object User {
    def withSaltedPassword(email: String, password: String, role: Role = Role.NORMAL) = {
      // we won't name this method as `apply` because it will cause problems with tupled definition. I'll try to
      // avoid method overloading from now on.
      val salt = newSalt
      val saltedPassword = applySalt(password, salt)
      User(None, email, saltedPassword, salt, role)
    }

    private[this] def newSalt = {
      val uuid = UUID.randomUUID()
      uuid.toString().replace("-", "")
    }

    def applySalt(password: String, salt: String): String = {
      val sha1 = MessageDigest.getInstance("SHA-1")
      val newPassword = password + salt
      sha1.update(newPassword.getBytes("UTF-8"))
      Hex.encodeHexString(sha1.digest())
    }

    def tupled = (User.apply _).tupled
  }

  case class Session(sessionId: String, userId: Int)

  class Sessions(tag: Tag) extends Table[Session](tag, "sessions") {
    def sessionId = column[String]("sessionid", O.PrimaryKey)

    def userId = column[Int]("userid")

    def user = foreignKey("USR_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (sessionId, userId) <> (Session.tupled, Session.unapply)
  }

  val sessions = TableQuery[Sessions]

  object Session {
    def createNewForUser(user: User): Session = {
      val uuid = UUID.randomUUID()
      val sessionId = uuid.toString().replace("-", "")
      Session(sessionId, user.id.get)
    }

    def tupled = (Session.apply _).tupled
  }

  implicit val datesMapper = MappedColumnType.base[LocalDate, Date](
    l => Date.valueOf(l),
    d => d.toLocalDate
  )

  case class Trip(id: Option[Int], destination: String, startDate: LocalDate, endDate: LocalDate, comment: String, userId: Int)

  class Trips(tag: Tag) extends Table[Trip](tag, "trip") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def destination = column[String]("destination", O.Length(120))

    def startDate = column[LocalDate]("start_date")

    def endDate = column[LocalDate]("end_date")

    def comment = column[String]("comment", O.Length(500))

    def userId = column[Int]("user_id")

    def user = foreignKey("TRP_USR_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, destination, startDate, endDate, comment, userId) <> (Trip.tupled, Trip.unapply)
  }

  val trips = TableQuery[Trips]

  lazy val fullSchema = users.schema ++ sessions.schema ++ trips.schema
}
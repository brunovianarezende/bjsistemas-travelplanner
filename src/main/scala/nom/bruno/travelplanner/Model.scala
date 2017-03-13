package nom.bruno.travelplanner

import java.security.MessageDigest
import java.util.UUID

import org.apache.commons.codec.binary.Hex
import slick.jdbc.MySQLProfile.api._

object Tables {
  case class User(id: Option[Int], email: String, password: String, salt: String, role: String)

  class Users(tag: Tag) extends Table[User] (tag, "user") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("email", O.Unique, O.Length(120))
    def password = column[String]("password", O.Length(40))
    def salt = column[String]("salt", O.Length(32))
    def role = column[String]("role")
    def * = (id.?, email, password, salt, role) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  object User {
    def withSaltedPassword(email: String, password: String, role: String = "NORMAL") = {
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
}

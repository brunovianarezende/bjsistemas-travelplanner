package nom.bruno.travelplanner

import slick.jdbc.MySQLProfile.api._

object Tables {
  case class User(id: Option[Int], email: String, password: String, salt: String, role: String)

  class Users(tag: Tag) extends Table[User] (tag, "user") {
    def id = column[Int]("id", O.PrimaryKey)
    def email = column[String]("email")
    def password = column[String]("password")
    def salt = column[String]("salt")
    def role = column[String]("role")
    def * = (id.?, email, password, salt, role) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]
}

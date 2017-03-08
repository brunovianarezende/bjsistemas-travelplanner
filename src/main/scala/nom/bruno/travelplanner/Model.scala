package nom.bruno.travelplanner

import slick.jdbc.MySQLProfile.api._

object Tables {
  class Users(tag: Tag) extends Table[(Int, String, String, String, String)] (tag, "user") {
    def id = column[Int]("id", O.PrimaryKey)
    def email = column[String]("email")
    def password = column[String]("password")
    def salt = column[String]("salt")
    def role = column[String]("role")
    def * = (id, email, password, salt, role)
  }
}

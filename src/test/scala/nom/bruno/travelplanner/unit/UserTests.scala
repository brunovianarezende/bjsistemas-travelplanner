package nom.bruno.travelplanner.unit

import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.Tables.{Role, User}
import org.scalatest.FunSpec

class UserTests extends FunSpec {
  def u(id: Int, email: String, role: Role) = User(Some(id), email, "abc", "abc", role)

  val normal1 = u(1, "n1@u.com", Role.NORMAL)
  val normal2 = u(2, "n2@u.com", Role.NORMAL)
  val um1 = u(11, "u1@u.com", Role.USER_MANAGER)
  val um2 = u(12, "u2@u.com", Role.USER_MANAGER)
  val admin1 = u(21, "a1@u.com", Role.ADMIN)
  val admin2 = u(22, "a2@u.com", Role.ADMIN)

  describe("permission checks") {
    describe("can see") {
      describe("A NORMAL user") {
        it("can see its own data") {
          assert(true == normal1.canSee(normal1))
        }

        it("can't see other NORMAL users data") {
          assert(false == normal1.canSee(normal2))
        }

        it("can't see USER_MANAGER users data") {
          assert(false == normal1.canSee(um1))
        }

        it("can't see ADMIN users data") {
          assert(false == normal1.canSee(admin1))
        }
      }

      describe("A USER_MANAGER user") {
        it("can see its own data") {
          assert(true == um1.canSee(um1))
        }

        it("can see NORMAL users data") {
          assert(true == um1.canSee(normal1))
        }

        it("can see other USER_MANAGER users data") {
          assert(true == um1.canSee(um2))
        }

        it("can't see ADMIN users data") {
          assert(false == um1.canSee(admin1))
        }
      }

      describe("An ADMIN user") {
        it("can see its own data") {
          assert(true == admin1.canSee(admin1))
        }

        it("can see NORMAL users data") {
          assert(true == admin1.canSee(normal1))
        }

        it("can see USER_MANAGER users data") {
          assert(true == admin1.canSee(um1))
        }

        it("can see other ADMIN users data") {
          assert(true == admin1.canSee(admin2))
        }
      }
    }
  }
}

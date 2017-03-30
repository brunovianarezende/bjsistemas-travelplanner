package nom.bruno.travelplanner

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
          assert(normal1.canSee(normal1))
        }

        it("can't see other NORMAL users data") {
          assert(!normal1.canSee(normal2))
        }

        it("can't see USER_MANAGER users data") {
          assert(!normal1.canSee(um1))
        }

        it("can't see ADMIN users data") {
          assert(!normal1.canSee(admin1))
        }
      }

      describe("A USER_MANAGER user") {
        it("can see its own data") {
          assert(um1.canSee(um1))
        }

        it("can see NORMAL users data") {
          assert(um1.canSee(normal1))
        }

        it("can see other USER_MANAGER users data") {
          assert(um1.canSee(um2))
        }

        it("can't see ADMIN users data") {
          assert(!um1.canSee(admin1))
        }
      }

      describe("An ADMIN user") {
        it("can see its own data") {
          assert(admin1.canSee(admin1))
        }

        it("can see NORMAL users data") {
          assert(admin1.canSee(normal1))
        }

        it("can see USER_MANAGER users data") {
          assert(admin1.canSee(um1))
        }

        it("can see other ADMIN users data") {
          assert(admin1.canSee(admin2))
        }
      }
    }

    describe("can see trips") {
      describe("A NORMAL user") {
        it("can see its own trips") {
          assert(normal1.canSeeTripsFrom(normal1))
        }

        it("can't see other NORMAL users trips") {
          assert(!normal1.canSeeTripsFrom(normal2))
        }

        it("can't see USER_MANAGER users trips") {
          assert(!normal1.canSeeTripsFrom(um1))
        }

        it("can't see ADMIN users trips") {
          assert(!normal1.canSeeTripsFrom(admin1))
        }
      }

      describe("A USER_MANAGER user") {
        it("can see its own trips") {
          assert(um1.canSeeTripsFrom(um1))
        }

        it("can't see NORMAL users trips") {
          assert(!um1.canSeeTripsFrom(normal1))
        }

        it("can't see other USER_MANAGER users trips") {
          assert(!um1.canSeeTripsFrom(um2))
        }

        it("can't see ADMIN users trips") {
          assert(!um1.canSeeTripsFrom(admin1))
        }
      }

      describe("An ADMIN user") {
        it("can see its own trips") {
          assert(admin1.canSeeTripsFrom(admin1))
        }

        it("can see NORMAL users trips") {
          assert(admin1.canSeeTripsFrom(normal1))
        }

        it("can see USER_MANAGER users trips") {
          assert(admin1.canSeeTripsFrom(um1))
        }

        it("can see other ADMIN users trips") {
          assert(admin1.canSeeTripsFrom(admin2))
        }
      }
    }

    describe("change role or password") {
      describe("No kind of user") {
        it("can change its own role") {
          assert(!normal1.canChangeRole(normal1, Role.USER_MANAGER))
          assert(!um1.canChangeRole(um1, Role.NORMAL))
          assert(!admin1.canChangeRole(admin1, Role.NORMAL))
        }
      }

      describe("All kind of users") {
        it("can change their own password") {
          for (user: User <- Seq(normal1, um1, admin1)) {
            assert(user.canChangePassword(user))
          }

        }
      }

      describe("A NORMAL user") {
        it("can't change anything from any other user") {
          for (user: User <- Seq(normal2, um1, admin1)) {
            assert(!normal1.canChangePassword(user))
            assert(!normal1.canChangeRole(user, user.role))
          }
        }
      }

      describe("A USER_MANAGER user") {
        it("can change the password of NORMAL users") {
          assert(um1.canChangePassword(normal1))
        }

        it("can change the role of NORMAL users to USER_MANAGER") {
          assert(um1.canChangeRole(normal1, Role.USER_MANAGER))
        }

        it("can change the role of other USER_MANAGER to NORMAL") {
          assert(um1.canChangeRole(um2, Role.NORMAL))
        }

        it("can change the password of other USER_MANAGER") {
          assert(um1.canChangePassword(um2))
        }

        it("can't change the role of any kind of user to ADMIN") {
          assert(!um1.canChangeRole(normal1, Role.ADMIN))
          assert(!um1.canChangeRole(um2, Role.ADMIN))
        }

        it("can't change anything of ADMIN users") {
          assert(!um1.canChangeRole(admin1, Role.NORMAL))
          assert(!um1.canChangePassword(admin1))
        }
      }

      describe("An ADMIN user") {
        it("can change the password of all kind of users") {
          assert(admin1.canChangePassword(normal1))
          assert(admin1.canChangePassword(um1))
          assert(admin1.canChangePassword(admin2))
        }

        it("can change the role of any other user to any role") {
          assert(admin1.canChangeRole(normal1, Role.USER_MANAGER))
          assert(admin1.canChangeRole(normal1, Role.ADMIN))
          assert(admin1.canChangeRole(um1, Role.NORMAL))
          assert(admin1.canChangeRole(um1, Role.ADMIN))
          assert(admin1.canChangeRole(admin2, Role.NORMAL))
          assert(admin1.canChangeRole(admin2, Role.USER_MANAGER))
        }
      }
    }

    describe("can delete") {
      describe("No kind of user") {
        it("can delete itself") {
          for (user <- Seq(admin1, um1, normal1)) {
            assert(!user.canDelete(user))
          }
        }
      }

      describe("A normal user") {
        it("can't delete any other user") {
          for (user <- Seq(admin1, um1, normal1)) {
            assert(!normal2.canDelete(user))
          }
        }
      }

      describe("A user manager") {
        it("can delete normal users") {
          assert(um1.canDelete(normal1))
        }
        it("can't delete a user manager") {
          assert(!um1.canDelete(um2))
        }

        it("can't delete an admin") {
          assert(!um1.canDelete(admin1))
        }
      }

      describe("An admin") {
        it("can delete normal users") {
          assert(admin1.canDelete(normal1))
        }
        it("can delete a user manager") {
          assert(admin1.canDelete(um1))
        }

        it("can't delete an admin") {
          assert(!admin1.canDelete(admin1))
        }
      }
    }
  }
}
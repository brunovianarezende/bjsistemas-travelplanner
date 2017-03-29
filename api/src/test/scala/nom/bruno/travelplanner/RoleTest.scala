package nom.bruno.travelplanner

import nom.bruno.travelplanner.Tables.Role
import org.scalatest.FunSuite

class RoleTest extends FunSuite {
  test("ADMIN > USER_MANAGER") {
    assert(Role.ADMIN > Role.USER_MANAGER)
  }

  test("ADMIN > NORMAL") {
    assert(Role.ADMIN > Role.NORMAL)
  }

  test("USER_MANAGER > NORMAL") {
    assert(Role.USER_MANAGER > Role.NORMAL)
  }

  test("Roles equality") {
    for (role <- Seq(Role.NORMAL, Role.USER_MANAGER, Role.ADMIN)) {
      assert(role == role)
    }
  }

}

package nom.bruno.travelplanner.controllers

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice, Module}
import nom.bruno.travelplanner.Tables.{Role, User}
import nom.bruno.travelplanner.services.{TripsService, UsersService}
import nom.bruno.travelplanner.{Error, ErrorCodes, Result}
import org.json4s.jackson.JsonMethods.parse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait BaseApiTravelPlannerStackTest extends TravelPlannerTests with BeforeAndAfterEach {
  val ADMIN1 = "admin1@users.com"
  val ADMIN2 = "admin2@users.com"
  val USER_MANAGER1 = "usermanager1@users.com"
  val USER_MANAGER2 = "usermanager2@users.com"
  val NORMAL1 = "normal1@users.com"
  val NORMAL2 = "normal2@users.com"
  val PASSWORD = "password"

  val emailToUser = mutable.Map.empty[String, User]

  def u(email: String, withSalt: Boolean = true): User = {
    val role = {
      if (email.startsWith("a")) Role.ADMIN
      else if (email.startsWith("u")) Role.USER_MANAGER
      else Role.NORMAL
    }

    if (!emailToUser.contains(email)) {
      val id = emailToUser.size + 1
      emailToUser(email) = {
        if (withSalt) User.withSaltedPassword(email, PASSWORD, role).copy(id = Some(id))
        else User(Some(id), email, PASSWORD, "salt", role)
      }
    }
    emailToUser(email)
  }

  var ALL_USERS: List[User] = List.empty

  def withUsers(testCode: => Any): Unit = {
    val allUsers = mutable.ArrayBuffer.empty[User]
    when(usersService.getSessionUser(any())).thenReturn(Future {
      None
    })
    when(usersService.getUser(any())).thenReturn(Future {
      None
    })
    for (email <- Seq(NORMAL1, USER_MANAGER1, ADMIN1)) {
      val user = u(email)
      allUsers += user
      when(usersService.getSessionUser(xSessionIdFor(email))).thenReturn(Future {
        Some(user)
      })
      when(usersService.getUser(email)).thenReturn(Future {
        Some(user)
      })
    }
    for (email <- Seq(NORMAL2, USER_MANAGER2, ADMIN2)) {
      val user = u(email)
      allUsers += user
      when(usersService.getUser(email)).thenReturn(Future {
        Some(user)
      })
    }
    ALL_USERS = allUsers.toList.sortBy(_.email)
    testCode
  }

  val usersService: UsersService = mock(classOf[UsersService])
  val tripsService: TripsService = mock(classOf[TripsService])

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    for {
      (servlet, path) <- TravelPlannerStack.servletInstances(Guice.createInjector(createGuiceModule))
    } addFilter(servlet, path)
  }

  def createGuiceModule: Module = {
    new AbstractModule {
      override def configure(): Unit = {
        bind(classOf[UsersService]).toInstance(usersService)
        bind(classOf[TripsService]).toInstance(tripsService)
        bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(global)
      }
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    ALL_USERS = List.empty
    reset(usersService, tripsService)
  }

  def authHeader(xSessionId: String) = {
    Map("Cookie" -> s"X-Session-Id=$xSessionId")
  }

  def authHeaderFor(email: String) = {
    authHeader(xSessionIdFor(email))
  }

  def xSessionIdFor(email: String) = {
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + email takeRight (32)
  }

  def checkNotAuthenticatedError: Any = {
    {
      status should equal(401)
      val result = parse(body).extract[Result[_]]
      result.errors.get should be(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
    }
  }
}
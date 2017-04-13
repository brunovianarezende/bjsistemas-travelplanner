package nom.bruno.travelplanner.service

import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}
import nom.bruno.travelplanner.Tables.Role
import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.{Error, ErrorCodes, Result, Tables}
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, Matchers}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait BaseIntegrationRoutesTest extends FeatureSpec with Matchers with ScalatestRouteTest with JsonProtocol
  with BeforeAndAfterEach {
  lazy val db = Database.forConfig("mysql")

  val ADMIN1 = "admin1@users.com"
  val ADMIN2 = "admin2@users.com"
  val USER_MANAGER1 = "usermanager1@users.com"
  val USER_MANAGER2 = "usermanager2@users.com"
  val NORMAL1 = "normal1@users.com"
  val NORMAL2 = "normal2@users.com"
  val PASSWORD = "password"

  def getCookie: HttpCookie = {
    val setCookieOpt: Option[`Set-Cookie`] = header[`Set-Cookie`]
    setCookieOpt should not be (None)
    setCookieOpt.get.cookie
  }

  def authHeader(xSessionId: String): Cookie = {
    Cookie("X-Session-Id", xSessionId)
  }

  def authHeaderFor(email: String) = {
    authHeader(xSessionIdFor(email))
  }

  def xSessionIdFor(email: String) = {
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + email takeRight (32)
  }

  private[this] def setupRunTestCodeAndTearDown(actions: Seq[DBIOAction[_, NoStream, _]], testCode: => Any): Unit = {
    Await.result(db.run(DBIO.seq(actions: _*)), Duration.Inf)
    try {
      testCode
    }
    finally {
      val tearDownActions = DBIO.seq((Tables.users.delete))
      Await.result(db.run(tearDownActions), Duration.Inf)
    }
  }

  def withUsers(testCode: => Any): Unit = {
    setupRunTestCodeAndTearDown(defaultSetupActions, testCode)
  }

  private[this] def defaultSetupActions: Seq[DBIOAction[_, NoStream, _]] = {
    def authenticatedUser(email: String, role: Role) = {
      for {
        id <- (Tables.users returning Tables.users.map(_.id)) += Tables.User.withSaltedPassword(email, PASSWORD, role = role)
        _ <- Tables.sessions += Tables.Session(xSessionIdFor(email), id)
      } yield ()
    }

    def nonAuthenticatedUser(email: String, role: Role) = {
      Tables.users += Tables.User.withSaltedPassword(email, PASSWORD, role = role)
    }

    Seq(authenticatedUser(ADMIN1, Role.ADMIN),
      nonAuthenticatedUser(ADMIN2, Role.ADMIN),
      authenticatedUser(USER_MANAGER1, Role.USER_MANAGER),
      nonAuthenticatedUser(USER_MANAGER2, Role.USER_MANAGER),
      authenticatedUser(NORMAL1, Role.NORMAL),
      nonAuthenticatedUser(NORMAL2, Role.NORMAL)
    )
  }

  private val injector = Guice.createInjector(new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[Database]) toInstance (db)
      bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(Implicits.global)
    }
  })

  val routesService = injector.getInstance(classOf[AllRoutes])

  override protected def afterAll(): Unit = {
    super.afterAll()
    db.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val cleanUpActions = DBIO.seq((Tables.users.delete))
    Await.result(db.run(cleanUpActions), Duration.Inf)
  }

  def checkNotAuthenticatedError: Any = {
    {
      status.intValue should equal(401)
      val result = responseAs[Result[Unit]]
      result.errors.get should be(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
    }
  }
}
package nom.bruno.travelplanner.controllers

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}
import nom.bruno.travelplanner.Tables
import nom.bruno.travelplanner.Tables.Role.Role
import nom.bruno.travelplanner.Tables.{Role, User}
import org.json4s.Formats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraFeatureSpec
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait BaseTravelPlannerStackTest extends ScalatraFeatureSpec with BeforeAndAfterEach {
  protected implicit def executor: ExecutionContext = Implicits.global

  protected implicit val jsonFormats: Formats = TravelPlannerStack.jsonFormats

  lazy val db = Database.forConfig("mysql")

  val ADMIN1 = "admin1@users.com"
  val ADMIN2 = "admin2@users.com"
  val USER_MANAGER1 = "usermanager1@users.com"
  val USER_MANAGER2 = "usermanager2@users.com"
  val NORMAL1 = "normal1@users.com"
  val NORMAL2 = "normal2@users.com"
  val PASSWORD = "password"

  def authHeader(xSessionId: String) = {
    Map("Cookie" -> s"X-Session-Id=$xSessionId")
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

  def withAdditionalUsers(users: Seq[User])(testCode: => Any): Unit = {
    val newActions = Seq(Tables.users ++= users)
    val actions = defaultSetupActions ++ newActions
    setupRunTestCodeAndTearDown(actions, testCode)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val module = new AbstractModule {
      override def configure(): Unit = {
        implicit def executor: ExecutionContext = Implicits.global

        bind(classOf[Database]) toInstance (db)
        bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(executor)
      }
    }

    for {
      (servlet, path) <- TravelPlannerStack.servletInstances(Guice.createInjector(module))
    } addFilter(servlet, path)
    //    Await.result(db.run(DBIO.seq(Tables.fullSchema.create)), Duration.Inf)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    //    Await.result(db.run(DBIO.seq(Tables.fullSchema.drop)), Duration.Inf)
    db.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val cleanUpActions = DBIO.seq((Tables.users.delete))
    Await.result(db.run(cleanUpActions), Duration.Inf)
  }

  def putAsJson[A, B <: AnyRef](uri: String, body: B, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A = {
    put[A](uri, jsonBytes(body), headers) {
      f
    }
  }

  private[this] def jsonBytes[B <: AnyRef, A](body: B) = {
    write(body).getBytes("UTF-8")
  }

  def postAsJson[A, B <: AnyRef](uri: String, body: B, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A = {
    val newHeaders = Map("Content-Type" -> "application/json") ++ headers
    post[A](uri, write(body).getBytes("UTF-8"), newHeaders) {
      f
    }
  }

  def checkNotAuthenticatedError: Any = {
    {
      status should equal(401)
      val result = parse(body).extract[Result[_]]
      result.errors.get should be(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
    }
  }
}
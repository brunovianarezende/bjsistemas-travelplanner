package nom.bruno.travelplanner

import nom.bruno.travelplanner.controllers.{Error, ErrorCodes, Result, TravelPlannerStack}
import org.json4s.Formats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraFeatureSpec
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait BaseTravelPlannerStackTest extends ScalatraFeatureSpec with BeforeAndAfterEach {
  protected implicit def executor: ExecutionContext = global

  protected implicit val jsonFormats: Formats = TravelPlannerStack.jsonFormats

  lazy val db = Database.forConfig("mysql")

  override protected def beforeAll(): Unit = {
    for {
      (servlet, path) <- TravelPlannerStack.servletInstances(db)
    } addFilter(servlet, path)
    super.beforeAll()
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

  def checkAuthenticationFailed: Any = {
    {
      status should equal(401)
      val result = parse(body).extract[Result[_]]
      result.errors.get should be(List(Error(ErrorCodes.USER_NOT_AUTHENTICATED)))
    }
  }
}
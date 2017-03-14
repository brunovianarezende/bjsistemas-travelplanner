package nom.bruno.travelplanner

import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization.write
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraFeatureSpec
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

trait BaseTravelPlannerServletTest extends ScalatraFeatureSpec with BeforeAndAfterEach {
  protected implicit def executor: ExecutionContext = global

  implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  lazy val db = Database.forConfig("mysql")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
//    val schema = Tables.users.schema
//    Await.result(db.run(DBIO.seq(schema.create)), Duration.Inf)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
//    val schema = Tables.users.schema
//    Await.result(db.run(DBIO.seq(schema.drop)), Duration.Inf)
    db.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val cleanUpActions = DBIO.seq((Tables.users.delete))
    Await.result(db.run(cleanUpActions), Duration.Inf)
  }

  def putAsJson[A, B <: AnyRef](uri: String, body: B, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A = {
    put[A](uri, write(body).getBytes("UTF-8"), headers){f}
  }

}

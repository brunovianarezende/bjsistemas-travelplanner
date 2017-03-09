package nom.bruno.travelplanner

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.test.scalatest.ScalatraFeatureSpec
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

trait BaseTravelPlannerServletTest extends ScalatraFeatureSpec {
  protected implicit def executor: ExecutionContext =  global
  implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  lazy val db = Database.forConfig("mysql")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val schema = Tables.users.schema
    Await.ready(db.run(DBIO.seq(schema.create)), Duration.Inf)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    db.close()
  }

}

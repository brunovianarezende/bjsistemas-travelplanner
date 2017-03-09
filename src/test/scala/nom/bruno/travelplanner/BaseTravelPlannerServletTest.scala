package nom.bruno.travelplanner

import org.scalatra.test.scalatest.ScalatraFeatureSpec
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait BaseTravelPlannerServletTest extends ScalatraFeatureSpec {
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

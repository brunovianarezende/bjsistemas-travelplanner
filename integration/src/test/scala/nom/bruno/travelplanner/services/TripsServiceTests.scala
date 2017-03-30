package nom.bruno.travelplanner.services

import java.sql.Date
import java.time.LocalDate

import nom.bruno.travelplanner.Tables
import nom.bruno.travelplanner.Tables.{Trip, User}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class TripsServiceTests extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {
  val logger = LoggerFactory.getLogger(getClass)

  protected implicit def executor: ExecutionContext = Implicits.global

  protected implicit def localDate2sqlDate(x: LocalDate): Date = Date.valueOf(x)

  lazy val db = Database.forConfig("mysql")
  var user1: User = Tables.User.withSaltedPassword("email@email.com", "password")
  var user2: User = Tables.User.withSaltedPassword("email2@email.com", "password")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    //    Await.result(db.run(DBIO.seq(Tables.trips.schema.create)), Duration.Inf)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    db.close()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val addUsers = (Tables.users returning Tables.users.map(_.id)) ++= Seq(user1, user2)
    val usersIds = Await.result(db.run(addUsers), Duration.Inf)
    this.user1 = this.user1.copy(id = Some(usersIds(0)))
    this.user2 = this.user2.copy(id = Some(usersIds(1)))
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    val cleanUpActions = DBIO.seq(Tables.users.delete)
    Await.result(db.run(cleanUpActions), Duration.Inf)
  }

  test("add trip") {
    val tripsService = new TripsService(db)
    val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place", user1.id.get)
    val id = Await.result(tripsService.addTrip(trip), Duration.Inf)
    assert(id > 0)
    val expectedTrip = trip.copy(id = Some(id))
    val allTrips = Await.result(db.run(Tables.trips.result), Duration.Inf)
    assert(allTrips == Seq(expectedTrip))
  }

  test("get trip") {
    val tripsService = new TripsService(db)
    val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place", user1.id.get)
    val id = Await.result(tripsService.addTrip(trip), Duration.Inf)
    assert(id > 0)
    val expectedTrip = trip.copy(id = Some(id))
    val retrievedTrip = Await.result(tripsService.getUserTrip(user1, id), Duration.Inf)
    assert(retrievedTrip.contains(expectedTrip))
  }

  test("get trip - doesn't exist") {
    val tripsService = new TripsService(db)
    val retrievedTrip = Await.result(tripsService.getUserTrip(user2, 0), Duration.Inf)
    assert(retrievedTrip.isEmpty)
  }

  test("get trip - from different user") {
    val tripsService = new TripsService(db)
    val trip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
      user1.id.get)
    val id = Await.result(tripsService.addTrip(trip), Duration.Inf)
    assert(id > 0)
    val retrievedTrip = Await.result(tripsService.getUserTrip(user2, id), Duration.Inf)
    assert(retrievedTrip.isEmpty)
  }


  test("search trips")(pending)

  test("update trip") {
    val tripsService = new TripsService(db)
    val baseTrip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
      user1.id.get)
    val id = Await.result(tripsService.addTrip(baseTrip), Duration.Inf)
    assert(id > 0)


    val updatedTrip = Trip(Some(id), "Other place", LocalDate.of(2017, 1, 2), LocalDate.of(2017, 1, 14),
      "not so nice place", user1.id.get)
    Await.result(tripsService.updateTrip(updatedTrip), Duration.Inf)

    val allTrips = Await.result(db.run(Tables.trips.result), Duration.Inf)
    assert(allTrips == Seq(updatedTrip))
  }

  test("delete trip") {
    val tripsService = new TripsService(db)
    val baseTrip = Trip(None, "Belo Horizonte", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 15), "nice place",
      user1.id.get)
    val id = Await.result(tripsService.addTrip(baseTrip), Duration.Inf)

    Await.result(tripsService.deleteTrip(baseTrip.copy(id = Some(id))), Duration.Inf)

    val allTrips = Await.result(db.run(Tables.trips.result), Duration.Inf)
    assert(allTrips.isEmpty)
  }
}

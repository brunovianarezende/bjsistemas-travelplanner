package nom.bruno.travelplanner.services

import nom.bruno.travelplanner.Tables.{Trip, User, trips, users}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class TripsService(val db: Database)(implicit val executionContext: ExecutionContext) {
  def getUserTrip(user: User, id: Int): Future[Option[Trip]] = {
    val q = trips.filter(_.userId === user.id).filter(_.id === id)
    db.run(q.result.headOption)
  }

  def addTrip(trip: Trip): Future[Int] = {
    db.run((trips returning trips.map(_.id)) += trip)
  }

  def updateTrip(updatedTrip: Trip): Future[Int] = {
    val q = for {
      t <- trips if t.id === updatedTrip.id
    } yield {
      (t.destination, t.startDate, t.endDate, t.comment)
    }
    val updateAction = q.update(updatedTrip.destination, updatedTrip.startDate, updatedTrip.endDate,
      updatedTrip.comment)
    db.run(updateAction)
  }

  def deleteTrip(trip: Trip): Future[Int] = {
    val q = trips.filter(_.id === trip.id)
    db.run(q.delete)
  }
}

package nom.bruno.travelplanner.services

import javax.inject.{Inject, Named}

import nom.bruno.travelplanner.Tables.{Trip, User}
import nom.bruno.travelplanner.repositories.TripsRepository

import scala.concurrent.{ExecutionContext, Future}

class TripsService @Inject()(tripsRepository: TripsRepository)
                            (@Named("EC") implicit val executionContext: ExecutionContext) {
  def getUserTrip(user: User, tripId: Int): Future[Option[Trip]] = {
    tripsRepository.getUserTrip(user, tripId)
  }

  def addTrip(trip: Trip): Future[Int] = {
    tripsRepository.addTrip(trip)
  }

  def updateTrip(updatedTrip: Trip): Future[Int] = {
    tripsRepository.updateTrip(updatedTrip)
  }

  def deleteTrip(trip: Trip): Future[Int] = {
    tripsRepository.deleteTrip(trip)
  }
}

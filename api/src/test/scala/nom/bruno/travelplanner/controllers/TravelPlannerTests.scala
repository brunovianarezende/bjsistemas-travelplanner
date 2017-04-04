package nom.bruno.travelplanner.controllers

import org.json4s.Formats
import org.json4s.jackson.Serialization.write
import org.scalatra.test.scalatest.ScalatraFeatureSpec

trait TravelPlannerTests extends ScalatraFeatureSpec {
  protected implicit val jsonFormats: Formats = TravelPlannerStack.jsonFormats

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
}

package nom.bruno.travelplanner

case class Flower(slug: String, name: String)

object FlowerData {

  /**
    * Some fake flowers data so we can simulate retrievals.
    */
  var all = List(
    Flower("yellow-tulip", "Yellow Tulip"),
    Flower("red-rose", "Red Rose"),
    Flower("black-rose", "Black Rose"))
}

class TravelPlannerServlet extends TravelPlannerStack {
  get("/") {
    FlowerData.all
  }

}

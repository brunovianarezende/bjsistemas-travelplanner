package nom.bruno.travelplanner

import org.scalatra._

class TravelPlannerServlet extends TravelPlannerStack {

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

}

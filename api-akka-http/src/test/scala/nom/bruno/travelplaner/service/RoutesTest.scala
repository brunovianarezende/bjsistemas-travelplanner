package nom.bruno.travelplaner.service

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}
import nom.bruno.travelplanner.service.{JsonProtocol, Routes, UserView}
import org.scalatest.{FeatureSpec, Matchers}

import scala.concurrent.ExecutionContext

class RoutesTest extends FeatureSpec with Matchers with ScalatestRouteTest with JsonProtocol {
  private val injector = Guice.createInjector(new AbstractModule {
    override def configure(): Unit = {
      // there is an annoying bug in intellij that will wrongly highlight all the checks as compilation errors if we
      // import global at the top of the file, so I'll import it here.
      import scala.concurrent.ExecutionContext.Implicits.global
      bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(global)
    }
  })

  private val routesService = injector.getInstance(classOf[Routes])

  feature("get all users") {
    scenario("user authenticated") {
      Get("/users") ~> routesService.routes ~> check {
        responseAs[UserView] should be(UserView("hi", "there"))
      }
    }
  }
}

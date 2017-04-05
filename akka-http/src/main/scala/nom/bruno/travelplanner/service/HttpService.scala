package nom.bruno.travelplanner.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object HttpService extends App {
  private val injector = Guice.createInjector(new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(global)
    }
  })

  private val routesService = injector.getInstance(classOf[Routes])

  implicit val system: ActorSystem = ActorSystem()

  implicit val materializer = ActorMaterializer()

  implicit val dispatcher = system.dispatcher

  Http().bindAndHandle(routesService.routes, "localhost", 8000)
}

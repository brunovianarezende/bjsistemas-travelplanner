import javax.servlet.ServletContext

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}
import nom.bruno.travelplanner.controllers.TravelPlannerStack
import nom.bruno.travelplanner.services.{AuthenticationService, TripsService, UsersService}
import org.scalatra._
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.{Database, DatabaseDef}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits

class ScalatraBootstrap extends LifeCycle {
  val logger = LoggerFactory.getLogger(getClass)

  var db: DatabaseDef = _

  override def init(context: ServletContext) {
    logger.info("Creating db connection")
    db = Database.forConfig("mysql")

    val module = new AbstractModule {
      override def configure(): Unit = {
        implicit def executor: ExecutionContext = Implicits.global

        bind(classOf[Database]).toInstance(db)
        bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(executor)
      }
    }

    for {
      (servlet, path) <- TravelPlannerStack.servletInstances(Guice.createInjector(module))
    } context.mount(servlet, path)
  }

  private def closeDbConnection(): Unit = {
    logger.info("Closing db connection")
    db.close()
  }

  override def destroy(context: ServletContext) {
    super.destroy(context)
    closeDbConnection
  }
}

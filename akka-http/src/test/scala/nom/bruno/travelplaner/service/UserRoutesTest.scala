package nom.bruno.travelplaner.service

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}
import nom.bruno.travelplanner.Tables.User
import nom.bruno.travelplanner.service.{JsonProtocol, Routes}
import nom.bruno.travelplanner.services.{AuthenticationService, TripsService, UsersService}
import nom.bruno.travelplanner.{Error, ErrorCodes, NewUserData, Result}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{mock, when, _}
import org.scalatest.{FeatureSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class UserRoutesTest extends FeatureSpec with Matchers with ScalatestRouteTest with JsonProtocol {
  val usersService: UsersService = mock(classOf[UsersService])
  val authenticationService: AuthenticationService = mock(classOf[AuthenticationService])
  val tripsService: TripsService = mock(classOf[TripsService])

  private val injector = Guice.createInjector(new AbstractModule {
    override def configure(): Unit = {
      // there is an annoying bug in intellij that will wrongly highlight all the checks as compilation errors if we
      // import global at the top of the file, so I'll import it here.
      import scala.concurrent.ExecutionContext.Implicits.global
      bind(classOf[ExecutionContext]).annotatedWith(Names.named("EC")).toInstance(global)
      bind(classOf[UsersService]).toInstance(usersService)
      bind(classOf[AuthenticationService]).toInstance(authenticationService)
      bind(classOf[TripsService]).toInstance(tripsService)
    }
  })

  private val routesService = injector.getInstance(classOf[Routes])

  feature("add users") {
    scenario("all ok") {
      when(usersService.addUser(any())).thenReturn(Future {})
      when(usersService.getUser(any())).thenReturn(Future {
        None
      })
      Put("/users/brunore@email.com", NewUserData("apassword", "apassword")) ~> routesService.routes ~> check {
        status.intValue should equal(200)
        responseAs[Result[Unit]].success should be(true)
        val captor: ArgumentCaptor[User] = ArgumentCaptor.forClass(classOf[User])
        verify(usersService, times(1)).addUser(captor.capture())
        captor.getValue.email should be("brunore@email.com")
      }
    }

    scenario("bad schema") {
      for (badInput <- List(
        """{}""",
        """[]""",
        """1""",
        """{"password": "!1APassword"}""",
        """{"password_confirmation": "!1APassword"}"""
      )) {
        Put("/users/brunore@email.com", HttpEntity(`application/json`, badInput)) ~>
          routesService.routes ~> check {
          status.intValue should equal(400)
          val result = responseAs[Result[Unit]]
          result.success should be(false)
          result.errors.get should be(List(Error(ErrorCodes.BAD_SCHEMA)))
        }
      }
    }

    scenario("invalid password") {
      Put("/users/brunore@email.com", NewUserData("abc", "abc")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD)))
      }
    }

    scenario("wrong confirmation") {
      Put("/users/brunore@email.com", NewUserData("abcdefg", "1234567")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_PASSWORD_CONFIRMATION)))
      }
    }

    scenario("invalid email") {
      Put("/users/brunore", NewUserData("apassword", "apassword")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.INVALID_EMAIL)))
      }
    }

    scenario("user already registered") {
      when(usersService.getUser(any())).thenReturn(Future {
        Some(User.withSaltedPassword("brunore@email.com", "apassword"))
      })

      Put("/users/brunore@email.com", NewUserData("apassword", "apassword")) ~>
        routesService.routes ~> check {
        status.intValue should equal(400)
        val result = responseAs[Result[Unit]]
        result.success should be(false)
        result.errors.get should be(List(Error(ErrorCodes.USER_ALREADY_REGISTERED)))
      }
    }

  }

}

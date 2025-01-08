package com.rockthejvm.reviewboard.http.controllers

import zio.*
import zio.test.*
import zio.json.*
import sttp.tapir.server.stub.TapirStubInterpreter

import sttp.client3.*
import sttp.monad.MonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.ztapir.RIOMonadError
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.domain.data.Company

object CompanyControllerSpec extends ZIOSpecDefault {

  // 'monad error' => a capability to map, flatMap & throw errors
  // needed to create the backendStub used in the test...
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]
                                                  // a monad error with a REQUIREMENT,
                                                  // which happens to be Any in that case

  private val rtjvm = Company(id=1, name="Rock the JVM", slug="rock-the-jvm", url="rockthejvm.com")

  private val serviceStub = new CompanyService {

    override def create(req: CompanyCreationRequest): Task[Company] = ZIO.succeed(rtjvm)

    override def getAll: Task[List[Company]] = ZIO.succeed(List(rtjvm))

    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed {
      if id == 1 then Some(rtjvm) else None
    }
    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed {
      if slug == rtjvm.slug then Some(rtjvm) else None
    }

  }

  private def backendStubZIO(getEndpoint: CompanyController => ServerEndpoint[Any, Task]) = for {
      // 1. CREATE THE CONTROLLER
      controller <- CompanyController.makeZIO
      // 2. Build TAPIR BACKEND (wrapped in a ZIO Effect)
      backendStub <- ZIO.succeed(
            TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
              // Customize Behaviors
              .whenServerEndpointRunLogic(getEndpoint(controller))
              .backend()
          )
    } yield backendStub


  def spec: Spec[
              TestEnvironment & Scope,
              Any // the potential Error
              ] =

    /*
      * We are going to SIMULATE THE ENDPOINTS via a "TAPIR STUB"
      * i.e, a TAPIR BACKEND, invoked synchronously with some HTTP Requests
      * The Responses to which, we are going to PARSE & INSPECT
      *
      *
      A TAPIR stub is a test double that simulates HTTP endpoints without running a real server.
      It lets you write tests that make HTTP-like requests and verify responses,
      but everything runs in memory synchronously - making your tests fast and reliable.
      Essentially, it's a way to test your API endpoints by intercepting requests and providing predetermined responses,
      all using TAPIR's type-safe endpoint definitions.
      *
      *
      */


    suite("CompanyControllerSpec")(

      test("post company") {

        val program = for {

          backendStub <- backendStubZIO(
                            // lambda to extract the endpoint under test
                            controller => controller.create
                          )

          // 3. Run HTTP Request
          response <- basicRequest
                      .post(uri"/companies") // uri"" custom string interpolator from sttp.client3
                      .body( // requires a string as input
                        // we must therefore SERIALIZE TO JSON using zio.json's extension methods
                        CompanyCreationRequest("Rock the JVM", "rockthejvm.com").toJson)
                        // toJson requires an implicit encoder, found in generator in sttp.tapir.generic.auto.*
                      .send(backendStub) // synchronously send this request to the backendstub

        } yield response.body // a string


          // 4. Inspect HTTP Responses
          // NB: .assert is a custom extension method we created to reduce boilerplate
          program.assert { respBody =>
              respBody
                .toOption // we do not care about the left hand side
                .flatMap(
                  // convert whatever string's inside into a company
                  _.fromJson[Company] // returns an Either
                  .toOption // to "stay within" the same option-monad context / structure
                ) // Option[Company]
                .contains(Company(id=1, name="Rock the JVM", slug="rock-the-jvm", url="rockthejvm.com"))

            }

      },

      test("get all") {

        val program = for {
              backendStub <- backendStubZIO(_.getAll)
              response <- basicRequest
                          .get(uri"/companies")
                          .send(backendStub)

            } yield response.body

        program.assert { respBody =>
            respBody // Either[String, String]
              .toOption
              .flatMap(_.fromJson[List[Company]].toOption) // Option[List[Company]]
              .contains(List(rtjvm))
          }

      },

      test("get by ID") {

        val program = for {
              backendStub <- backendStubZIO(_.getById)
              response <- basicRequest
                          .get(uri"/companies/1")
                          .send(backendStub)
            } yield response.body

        program.assert { respBody =>
            respBody // Either[String, String]
              .toOption
              .flatMap(_.fromJson[Company].toOption) // Option[Company]
              .contains(rtjvm)
          }

      },

  ).provide(ZLayer.succeed(serviceStub))
}



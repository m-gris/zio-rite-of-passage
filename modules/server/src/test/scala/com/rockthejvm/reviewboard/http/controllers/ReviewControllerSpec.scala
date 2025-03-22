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
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.repositories.ReviewRepositorySpec.badReview
import com.rockthejvm.reviewboard.repositories.ReviewRepositorySpec.goodReview

object ReviewControllerSpec extends ZIOSpecDefault {

  // 'monad error' => a capability to map, flatMap & throw errors
  // REQ: needed to create the backendStub used in the test...
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]
                                                  // a monad error with a REQUIREMENT,
                                                  // which happens to be Any in that case


  /*
   * REQUIREMENT for the "CONTROLLER UNDER TEST": we need a SERVICE
   */
  private val serviceStub = new ReviewService {

    override def create(request: ReviewCreationRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] = ZIO.succeed {
      List(goodReview, badReview).filter(_.id == id).headOption
    }

    override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
      List(goodReview, badReview).filter(_.companyId == companyId)
    }

    override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
      List(goodReview, badReview).filter(_.userId == userId)
    }

  }

  /*
   * REQ: a SYNCHRONOUS http server
   * allowing to send http requests as args to funcs
   * and getting back an http response SYNCHRONOUSLY
   */
  private def backendStubZIO(getEndpoint: ReviewController => ServerEndpoint[Any, Task]) = for {
      // 1. CREATE THE CONTROLLER
      controller <- ReviewController.makeZIO // requires an "injected" ReviewService
      // 2. Build TAPIR BACKEND (wrapped in a ZIO Effect)
      backendStub <- ZIO.succeed(
            /* the TapirStubInterpreter will run the controller's logic to those endpoints*/
            TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
              // Customize Behaviors
              .whenServerEndpointRunLogic(getEndpoint(controller))
              .backend()
          )
     /*will SYNCHRONOUSLY return http responses
      * as http requests are being pushed to that backendStub*/
    } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =

    suite("ReviewControllerSpec") (

      test("post review") {
        val program = for {
          // stub the backend
          backendStub <- backendStubZIO(
            // basically... what to do with the controller
            // that will be created on the basis of an "injected" ReviewService
            controller => controller.create)
          // make request (posting a review)
          response    <- basicRequest
                          .post(uri"/reviews")
                          .body(ReviewCreationRequest(
                                  companyId = 1L,
                                  management = 5,
                                  culture = 5,
                                  salary = 5,
                                  benefits = 5,
                                  wouldRecommend = 10,
                                  review = "all good"
                                )/*serialize*/.toJson // extension method enabled tapir.generic_auto
                               )
                          .send(backendStub)

        } yield response.body // Either[String, String] that will be "parsed from json" downstream

        program.assert {
          // CONVERT the response (Either[String, String]) to Option[String]
          _.toOption
          // PARSE
          .flatMap(
            _.fromJson[Review]/*Either*/.toOption // Option[Review]
          )
          // check that the Option[Review] contains the Review made in the request
          .contains(goodReview)
        }
      },

      test("get review") {
        for {
          backendstub       <- backendStubZIO(controller => controller.getById)
          response          <- basicRequest
                                .get(uri"/reviews/${goodReview.id}")
                                .send(backendstub)
          responseNotFound    <- basicRequest
                                .get(uri"/reviews/999")
                                .send(backendstub)
        } yield assertTrue {
          response.body.toOption.flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
          &&
          responseNotFound.body.toOption.flatMap(_.fromJson[Review].toOption)
            .isEmpty
        }

      },

      test("get company reviews") {
        for {
          backendstub       <- backendStubZIO(controller => controller.getByCompanyId)
          response          <- basicRequest
                                .get(uri"reviews/company/${goodReview.companyId}")
                                .send(backendstub)
          responseNotFound  <- basicRequest
                                .get(uri"reviews/company/5432")
                                .send(backendstub)
          _                 <- Console.printLine(s"Success response body: ${response.body}")
          _                 <- Console.printLine(s"Not found response body: ${responseNotFound.body}")
        } yield assertTrue {
          response.body.toOption.flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(goodReview, badReview))
          &&
          responseNotFound.body.toOption.flatMap(_.fromJson[List[Review]].toOption)
            .contains(List())
        }
      }


      ).provide(ZLayer.succeed(serviceStub))

}

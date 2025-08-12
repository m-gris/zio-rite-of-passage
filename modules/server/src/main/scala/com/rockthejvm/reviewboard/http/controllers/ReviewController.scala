package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.domain.data.Identifiers
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.http.requests.ReviewCreationRequest

class ReviewController private (reviewService: ReviewService, jwtService: JWTService)
      extends BaseController
      with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[Identifiers, Task](
        token => jwtService.verifyToken(token).either)
      .serverLogicSuccess(
        (user: Identifiers) => (req: ReviewCreationRequest) => reviewService.create(req, -1L /*TODO create user id*/)
    )

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess(
      id => /* the path that was automatically parsed by TAPIR */
        reviewService.getById(id)
      )

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogicSuccess( companyId => reviewService.getByCompanyId(companyId ))

  val getSummary: ServerEndpoint[Any, Task] =
    getSummaryEndpoint.serverLogicSuccess( companyId => reviewService.getSummary(companyId))

  val makeSummary: ServerEndpoint[Any, Task] =
    makeSummaryEndpoint.serverLogicSuccess( companyId => reviewService.makeSummary(companyId))


  override val routes: List[ServerEndpoint[Any, Task]] =
    List(getSummary, makeSummary, create, getById, getByCompanyId)

}


object ReviewController {

  /* EFFECTFUL 'smart constructor' */
  val makeZIO = for {
    jwtService    <- ZIO.service[JWTService]
    reviewService <- ZIO.service[ReviewService]
  } yield ReviewController(reviewService, jwtService)

}


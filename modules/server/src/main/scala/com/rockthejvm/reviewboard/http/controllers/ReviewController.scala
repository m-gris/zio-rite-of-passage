package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.services.ReviewService
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.http.requests.ReviewCreationRequest

class ReviewController private (reviewService: ReviewService)
      extends BaseController
      with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogicSuccess(
      (req: ReviewCreationRequest) => reviewService.create(req, -1L /*TODO create user id*/)
    )

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess(
      id => /* the path that was automatically parsed by TAPIR */
        reviewService.getById(id)
      )

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogicSuccess( companyId => reviewService.getByCompanyId(companyId ) )


  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getById, getByCompanyId)

}


object ReviewController {

  /* EFFECTFUL 'smart constructor' */
  val makeZIO = for {
    service <- ZIO.service[ReviewService]
  } yield ReviewController(service)

}


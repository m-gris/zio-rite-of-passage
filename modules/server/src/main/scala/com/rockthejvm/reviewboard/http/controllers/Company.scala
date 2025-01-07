package com.rockthejvm.reviewboard.http.controllers

import scala.collection.mutable

import zio.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.http.controllers.BaseController
import com.rockthejvm.reviewboard.services.*



class CompanyController private (service: CompanyService) extends /*i.e IMPLEMENTS */ BaseController with CompanyEndpoints {

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { request => // i.e the PAYLOAD of the POST
    service.create(request)
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess{ _ =>
    service.getAll
  }


  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess{ id =>  // nota: NOT A PAYLOAD, but a PATH PARAMETER
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById(_))
        .catchSome {
          case _: java.lang.NumberFormatException => service.getBySlug(id)
        }
      }

  override val routes = List(create, getAll, getById)
}


object CompanyController {
  // make effectfullness EXPLICIT
  val makeZIO = for {
    service <- ZIO.service[CompanyService]
  } yield CompanyController(service)
}


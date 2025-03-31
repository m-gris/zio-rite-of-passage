package com.rockthejvm.reviewboard.http.controllers

import scala.collection.mutable

import zio.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.Identifiers
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.http.controllers.BaseController



class CompanyController private (
  companyService: CompanyService,
  jwtService: JWTService
) extends /*i.e IMPLEMENTS */ BaseController with CompanyEndpoints {


  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[Identifiers, Task]{
          token => jwtService.verifyToken(token).either }
      .serverLogic {
        user /* serverSecurityLogic output */
          => request /* post's payload */
          => companyService.create(request).either
      }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic{ _ =>
    companyService.getAll.either
  }


  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic{ id =>  // nota: NOT A PAYLOAD, but a PATH PARAMETER
      ZIO
        .attempt(id.toLong)
        .flatMap(companyService.getById(_))
        .catchSome {
          case _: java.lang.NumberFormatException => companyService.getBySlug(id)
        }.either
      }

  override val routes = List(create, getAll, getById)

}


object CompanyController {
  // make effectfullness EXPLICIT
  val makeZIO = for {
    companyService <- ZIO.service[CompanyService]
    jwtService <- ZIO.service[JWTService]
  } yield CompanyController(companyService, jwtService)
}


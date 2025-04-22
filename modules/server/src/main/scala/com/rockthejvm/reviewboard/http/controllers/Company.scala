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


  val allFilters: ServerEndpoint[Any, Task] = allFiltersEndpoint.serverLogic { _ =>
   companyService.getAllFilters.either
  }

  val search: ServerEndpoint[Any, Task] = searchEndpoint.serverLogic { filter =>
      companyService.search(filter).either
    }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic{ id =>  // nota: NOT A PAYLOAD, but a PATH PARAMETER
      ZIO
        .attempt(id.toLong)
        .flatMap(companyService.getById(_))
        .catchSome {
          case _: java.lang.NumberFormatException => companyService.getBySlug(id)
        }.either
      }


  /*
   * Route order is critical for correct path matching:
   * - More specific routes must come before more general ones
   * - 'allFilters' must precede 'getById' because:
   *   - 'getById' matches any path pattern '/companies/{id}'
   *   - 'allFilters' uses path '/companies/filters'
   *   - if ordered incorrectly, '/companies/filters' would be captured by 'getById'
   *     with 'filters' interpreted as the {id} parameter
   */
  override val routes = List(create, getAll, allFilters, search, getById)

}


object CompanyController {
  // make effectfullness EXPLICIT
  val makeZIO = for {
    companyService <- ZIO.service[CompanyService]
    jwtService <- ZIO.service[JWTService]
  } yield CompanyController(companyService, jwtService)
}


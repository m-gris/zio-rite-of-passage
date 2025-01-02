package com.rockthejvm.reviewboard.http.controllers

import scala.collection.mutable

import zio.*

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints

private class CompanyController extends /*i.e IMPLEMENTS */CompanyEndpoints {

  // in-memory DB for now
  val db: mutable.Map[Long, Company] = mutable.Map()


  val create = createEndpoint.serverLogicSuccess { request => // i.e the PAYLOAD of the POST
    ZIO.succeed {
    val newId = db.keys.max + 1
    val newCompany = request.toCompany(newId)
    db += (newId -> newCompany)
    newCompany
    }
  }

  val getAll = getAllEndpoint.serverLogicSuccess( _ => ZIO.succeed(db.values.toList) )


  val getById = getByIdEndpoint.serverLogicSuccess{ id =>  // nota: NOT A PAYLOAD, but a PATH PARAMETER
      ZIO
        .attempt(id.toLong)
        .map(db.get)
      }

}


object CompanyController {
  // make effectfullness EXPLICIT
  val makeZIO = ZIO.succeed( new CompanyController )
}


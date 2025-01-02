package com.rockthejvm.reviewboard.http.endpoints

// IMPORTS - 3rd parties
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation functionallity (to create the JsonCodecs)

// IMPORTS - Local
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests


// NOTA BENE
// Here we are merely DESCRIBING the endpoints
// those will be implemented in / as so-called CONTROLLERS

trait CompanyEndpoints {

  val createEndpoint = endpoint
    .tag("companies")
    .name("create")
    .description("Create a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[requests.CreateCompany])
    .out(jsonBody[Company])

  val getAllEndpoints =
    endpoint
      .tag("companies")
      .name("getAll")
      .description("Get all company listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoints =
    endpoint
      .tag("companies")
      .name("getById")
      .description("Get a company listing by its ID")
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])


}

package com.rockthejvm.reviewboard.http.endpoints

// IMPORTS - 3rd parties
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation functionallity (to create the JsonCodecs)

// IMPORTS - Local
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.*


// NOTA BENE
// Here we are merely DESCRIBING the endpoints
// those will be implemented as SERVICES in so-called CONTROLLERS

trait CompanyEndpoints extends BaseEndpoint {

  val createEndpoint = securedBaseEndpoint
    .tag("companies")
    .name("create")
    .description("Create a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CompanyCreationRequest]) // TODO: search if Metals or another plugin can inlay Implicit params & implicit conversions
    .out(jsonBody[Company])

  val getAllEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getAll")
      .description("Get all company listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getById")
      .description("Get a company listing by its ID")
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])

  val allFiltersEndpoint =
    baseEndpoint
      .tag("companies")
      .name("allFilters")
      .description("Get all possible filters for companies search")
      .in("companies" / "filters")
      .get
      .out(jsonBody[CompanyFilter])

}

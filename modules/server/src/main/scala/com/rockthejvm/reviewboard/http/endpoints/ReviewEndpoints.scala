package com.rockthejvm.reviewboard.http.endpoints

// IMPORTS - 3rd parties
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation functionallity (to create the JsonCodecs)

// IMPORTS - Local
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.*

trait ReviewEndpoints extends BaseEndpoint {

  val createEndpoint = baseEndpoint
    .tag("reviews")
    .name("create")
    .description("Adds a review for a given company")
    .in("reviews")
    .post
    .in(jsonBody[ReviewCreationRequest])
    .out(jsonBody[Review])

  val getByIdEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("getById")
      .description("Get a review by its ID")
      .in("reviews" / path[Long]("id"))
      .get
      .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("getAll")
      .description("Get all reviews for a given company")
      .in("reviews" / "company" / path[Long]("id"))
      .get
      .out(jsonBody[List[Review]])

}

package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint extends BaseEndpoint {

  val healthEndpoint = baseEndpoint
          .tag("health")
          .name("health")
          .description("Health Check")
          .get
          .in("health")
          .out(plainBody[String])

  val errorEndpoint = baseEndpoint
          .tag("health")
          .name("error health")
          .description("Health Check - should fail")
          .get
          .in("health" / "error")
          .out(plainBody[String])

  }


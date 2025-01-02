package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint {
  val check = endpoint
          .tag("health")
          .name("health")
          .description("Health Check")
          .get
          .in("health")
          .out(plainBody[String])
  }

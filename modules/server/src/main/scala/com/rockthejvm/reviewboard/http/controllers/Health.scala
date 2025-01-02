package com.rockthejvm.reviewboard.http.controllers

import zio.*

import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint

private class HealthController extends HealthEndpoint {
  val health = check
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))
}

object HealthController {
  // making it clear / explicit
  // that instantiatin a controller is
  // an effectful operation
  val makeZIO = ZIO.succeed(new HealthController)
}



package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint

private class HealthController extends BaseController with HealthEndpoint {
  val health = check
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))
  override val routes = List(health)
}

object HealthController {
  // making it clear / explicit
  // that instantiatin a controller is
  // an effectful operation
  val makeZIO = ZIO.succeed(new HealthController)
}



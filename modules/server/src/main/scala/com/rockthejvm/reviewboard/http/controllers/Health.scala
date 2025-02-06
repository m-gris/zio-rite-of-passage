package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.errors.*
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint

private class HealthController extends BaseController with HealthEndpoint {

  val healthRoute = healthEndpoint
    // serverLogicSucces => guarantees that the endpoint returns a successfull effect.
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))


  val errorRoute = errorEndpoint

    // serverLogic => Can fail (as opposed to serverLogicSuccess)
    .serverLogic[Task](
      _ => ZIO.fail(new RuntimeException("Boom!!!")
                /* move the Throwable
                 * from the error channel
                 * to the value channel
                 * (as the Left side of an Either)
                 */
                ).either) // Task[Either[Throwable, String]]

  override val routes = List(healthRoute, errorRoute)

}

object HealthController {
  // making it clear / explicit
  // that instantiatin a controller is
  // an effectful operation
  val makeZIO = ZIO.succeed(new HealthController)
}



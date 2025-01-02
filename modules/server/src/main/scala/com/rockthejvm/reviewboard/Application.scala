package com.rockthejvm.reviewboard

import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*


object Application extends ZIOAppDefault {

  object EndPoints:

    val health = endpoint
            // this is an case of a 'builder' pattern
            .tag("health")
            .name("health")
            .description("Health Check")
            // ^^ this is for documentation (swager etc...)
            .get // the http method
            .in("health") // the path
            .out(plainBody[String]) // simple strings... nothing to parse or serialize

            .serverLogicSuccess[Task](_ /* the request */ => ZIO.succeed("All good"))

  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g. CORS for security etc...
      ).toHttp(
        EndPoints.health // the route that this server is going to serve
      )
    )

  override def run = serverProgram.provide(
    Server.default
    )

}

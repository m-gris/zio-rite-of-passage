package com.rockthejvm.reviewboard

import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*

import com.rockthejvm.reviewboard.http.controllers.*


object Application extends ZIOAppDefault {

  val serverProgram = for {
    controller <- HealthController.makeZIO
    _          <- Server.serve(
                    ZioHttpInterpreter(
                      ZioHttpServerOptions.default // can add configs e.g. CORS for security etc...
                      ).toHttp(
                        controller.health // the route that this server is going to serve
                      )
                    )
  } yield ()

  override def run = serverProgram.provide(
    Server.default
    )

}

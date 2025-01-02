package com.rockthejvm.reviewboard

import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*

import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.http.HttpAPI


object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints  <- HttpAPI.endpointsZIO
    _          <- Server.serve(
                    ZioHttpInterpreter(
                      ZioHttpServerOptions.default // can add configs e.g. CORS for security etc...
                      ).toHttp(endpoints) // the route that this server is going to serve
                    )
    _          <- Console.printLine("\n\n@@@@ Success - Server Started @@@\n\n")
  } yield ()

  override def run = serverProgram.provide(
    Server.default
    )

}

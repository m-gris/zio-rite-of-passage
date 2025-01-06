package com.rockthejvm.reviewboard

import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*

import com.rockthejvm.reviewboard.http.HttpAPI
import com.rockthejvm.reviewboard.http.controllers.*
import java.io.IOException


object Application extends ZIOAppDefault {

  val serverProgram: ZIO[Server, IOException, Unit] = for {
    endpoints  <- HttpAPI.endpointsZIO
    httpApp = ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(endpoints)
    _          <- Server.serve(httpApp) // provided / injected below...
    _          <- Console.printLine("\n\n@@@@ Success - Server Started @@@\n\n")
  } yield ()

  override def run = serverProgram.provide(Server.default)

}

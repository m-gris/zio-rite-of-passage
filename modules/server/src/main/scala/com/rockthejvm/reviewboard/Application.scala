package com.rockthejvm.reviewboard

import java.io.IOException

import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*

import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.http.HttpAPI
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.http.controllers.*



object Application extends ZIOAppDefault {

  // val serverProgram: ZIO[Server, IOException, Unit] = for {
  val serverProgram = for {
    endpoints  <- HttpAPI.endpointsZIO
    httpApp    = ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(endpoints)
    _          <- Server.serve(httpApp) // provided / injected below...
    _          <- Console.printLine("\n\n@@@@ Success - Server Started @@@\n\n")
  } yield ()

  override def run = serverProgram.provide(
    Server.default,
    CompanyServiceLive.layer,
    CompanyRepositoryLive.layer,
    ReviewServiceLive.layer,
    ReviewRepositoryLive.layer,
    Repository.dataLayer,
  )

}

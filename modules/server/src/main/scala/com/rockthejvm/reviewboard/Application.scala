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
import sttp.tapir.server.interceptor.cors.CORSInterceptor



object Application extends ZIOAppDefault {

  val serverProgram = for {

    endpoints  <- HttpAPI.endpointsZIO

    httpApp    = ZioHttpInterpreter(
                  ZioHttpServerOptions
                    .default
                    // Add CORS support to allow frontend (localhost:1234)
                    // to access this API
                    .appendInterceptor(CORSInterceptor.default)
                    // Without this:
                    // "Access blocked by CORS policy:
                    // No 'Access-Control-Allow-Origin' header"
                ).toHttp(endpoints)

    _          <- Server.serve(httpApp) // provided / injected below...

    _          <- Console.printLine("\n\n@@@@ Success - Server Started @@@\n\n")

  } yield ()

  override def run = serverProgram.provide(

    Server.default,

    // SERVICES
    UserServiceLive.layer,
    ReviewServiceLive.layer,
    CompanyServiceLive.layer,
    JWTServiceLive.configuredLayer,
    EmailServiceLive.configuredLayer,

    // REPOS
    Repository.dataLayer,
    UserRepositoryLive.layer,
    ReviewRepositoryLive.layer,
    CompanyRepositoryLive.layer,
    OTPRepositoryLive.configuredLayer,
  )

}

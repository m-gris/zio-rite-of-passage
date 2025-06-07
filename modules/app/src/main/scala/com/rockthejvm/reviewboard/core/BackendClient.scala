package com.rockthejvm.reviewboard.core

import zio.*
import sttp.client3.*
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter

import com.rockthejvm.reviewboard.http.endpoints.*
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.config.BackendClientConfig

trait BackendClient:
  def companyEndpoints: CompanyEndpoints
  def userEndpoints: UserEndpoints
  def sendRequestZIO[I,E<:Throwable,O]
        (endpoint: Endpoint[Unit, I, E, O, Any])
        (payload: I): Task[O]

class BackendClientLive (
  backend: SttpBackend[Task, ZioStreams & WebSockets],
  interpreter: SttpClientInterpreter,
  config: BackendClientConfig
  ) extends BackendClient {

  val companyEndpoints =
    // reminder: this create an ANONYMOUS CLASS
    // that implements the `CompanyEndpoints` trait
    new CompanyEndpoints {} // no abstract method... no override...

  val userEndpoints = new UserEndpoints {}


  private def prepareRequest[I,E,O](endpoint: Endpoint[Unit, I, E, O, Any]):
    I => Request[Either[E, O], Any] =
      interpreter
        .toRequestThrowDecodeFailures(endpoint, config.uri)

  override def sendRequestZIO[I,E<:Throwable,O]
        (endpoint: Endpoint[Unit, I, E, O, Any])
        (payload: I): Task[O] =

      backend.send(prepareRequest(endpoint)(payload))
              .map(response => response.body)
              // submerge failures with ZIO.absolve (the opposite of either)
              // turning a ZIO[R, Nothing, Either[E, A]] into a ZIO[R, E, A]
              .absolve
}

object BackendClientLive {

  val layer = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams & WebSockets]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(backend, interpreter, config)
  }

  val configuredLayer = {
    val backend = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val config = BackendClientConfig(Some(uri"http://localhost:8080"))

    ZLayer.succeed(backend) ++
    ZLayer.succeed(interpreter) ++
    ZLayer.succeed(config) >>> layer

  }

}

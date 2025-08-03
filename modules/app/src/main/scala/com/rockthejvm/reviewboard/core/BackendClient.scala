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
import com.rockthejvm.reviewboard.domain.data.UserSession


case class RestrictedEndpointException(message: String) extends RuntimeException(message)

trait BackendClient:
  def companyEndpoints: CompanyEndpoints
  def userEndpoints: UserEndpoints
  def reviewEndpoints: ReviewEndpoints
  def inviteEndpoints: InviteEndpoints
  def sendRequestZIO[I,E<:Throwable,O]
        (endpoint: Endpoint[Unit, I, E, O, Any]) // un-secured -> SECURITY_INPUT: Unit
        (payload: I): Task[O]

  def secureSendRequestZIO[I,E<:Throwable,O]
        (endpoint: Endpoint[String, I, E, O, Any]) // secured -> SECURITY_INPUT: String (will be our JWT ???)
        (payload: I): Task[O]

class BackendClientLive (
  backend: SttpBackend[Task, ZioStreams & WebSockets],
  interpreter: SttpClientInterpreter,
  config: BackendClientConfig
  ) extends BackendClient {

  // NOTE:
  // Scala syntax reminder:
  // `new TraitName {}` creates an anonymous class that implements the trait
  // The {} is the body of the anonymous class where you'd normally put overrides
  // When the trait has all methods already implemented, the body can remain empty
  // This pattern is commonly used to get an instance of a trait without creating a named class
  val companyEndpoints = // reminder: this create an ANONYMOUS CLASS that implements the `CompanyEndpoints` trait
    new CompanyEndpoints {} // no abstract method... no override...
  val userEndpoints = new UserEndpoints {}
  val reviewEndpoints = new ReviewEndpoints {}
  val inviteEndpoints = new InviteEndpoints {}

  private val tokenOrFail = ZIO.fromOption(Session.getUserState())
                               .orElseFail(RestrictedEndpointException("You need to log in."))
                               .map( (s: UserSession) => s.token )

  private def prepareRequest[I,E,O](endpoint: Endpoint[Unit, I, E, O, Any]):
    I => Request[Either[E, O], Any] =
      interpreter
        .toRequestThrowDecodeFailures(endpoint, config.uri)

  private def prepareSecureRequest[S,I,E,O](endpoint: Endpoint[S,I,E,O,Any]):
  // token -> payload -> request
    S  => I  => Request[Either[E, O], Any] =
      interpreter
        .toSecureRequestThrowDecodeFailures(endpoint, config.uri)


  override def sendRequestZIO[I,E<:Throwable,O]
        (endpoint: Endpoint[Unit, I, E, O, Any])
        (payload: I): Task[O] =

      backend.send(prepareRequest(endpoint)(payload))
              .map(response => response.body)
              // submerge failures with ZIO.absolve (the opposite of either)
              // turning a ZIO[R, Nothing, Either[E, A]] into a ZIO[R, E, A]
              .absolve

  override def secureSendRequestZIO[I,E<:Throwable,O]
        (endpoint: Endpoint[String, I, E, O, Any])
        (payload: I): Task[O] = {
          for {

            token    <- tokenOrFail
            response <- backend.send(prepareSecureRequest(endpoint)(token)(payload))
                    .map(response => response.body)
                    // submerge failures with ZIO.absolve (the opposite of either)
                    // turning a ZIO[R, Nothing, Either[E, A]] into a ZIO[R, E, A]
                    .absolve
          } yield response
        }


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

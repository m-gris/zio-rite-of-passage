package com.rockthejvm.reviewboard.services

import zio.*
import sttp.tapir.*
import sttp.client3.*
import sttp.model.Uri
import sttp.tapir.json.zio.*
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.client3.httpclient.zio.HttpClientZioBackend

import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.domain.errors.HttpError
import com.rockthejvm.reviewboard.http.requests.LLMCallRequest
import com.rockthejvm.reviewboard.http.responses.LLMCallResponse
import com.rockthejvm.reviewboard.config.SummarizationConfig
import com.rockthejvm.reviewboard.http.endpoints.SummarizationEndpoints


trait SummarizationService {
  def llmCall(prompt: String): Task[Option[String]]
}

class SummarizationServiceLive private(
  backend: SttpBackend[Task, ZioStreams],
  interpreter: SttpClientInterpreter,
  config: SummarizationConfig
  ) extends SummarizationService with SummarizationEndpoints {

  private def prepareSecureRequest[S,I,E,O](endpoint: Endpoint[S,I,E,O,Any]):
  //token -> payload -> request
    S  => I  => Request[Either[E, O], Any] =
      interpreter
        .toSecureRequestThrowDecodeFailures(endpoint, Uri.parse(config.baseUrl).toOption)

  def secureSendRequestZIO[I,E<:Throwable,O]
    (endpoint: Endpoint[String, I, E, O, Any])
    (payload: I): Task[O] =
    backend
      .send(prepareSecureRequest(endpoint)(config.key)(payload))
      .map(response => response.body)
      // submerge failures with ZIO.absolve (the opposite of either)
      // turning a ZIO[R, Nothing, Either[E, A]] into a ZIO[R, E, A]
      .absolve

  def llmCall(prompt: String): Task[Option[String]] =
    secureSendRequestZIO(llmCallEndpoint)(LLMCallRequest.single(prompt))
      .map( (response: LLMCallResponse) => response.choices.map(_.message.content))
      .map(_.headOption)

}

object SummarizationServiceLive {
  val layer = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[SummarizationConfig]
      service     <- ZIO.succeed(new SummarizationServiceLive(backend, interpreter, config))
    } yield service
  }

  val configuredLayer =
    HttpClientZioBackend.layer()
      >+> ZLayer.succeed(SttpClientInterpreter())
      >+> Configs.makeLayer[SummarizationConfig]("rockthejvm.summarization")
      >>> layer
}

object SummarizationServiceDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    ZIO.service[SummarizationService]
      .flatMap(_.llmCall("Please write a non-trivial joke about monads"))
      .flatMap(resp => Console.printLine(resp.toString))
      .provide(SummarizationServiceLive.configuredLayer)
}

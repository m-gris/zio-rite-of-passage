package com.rockthejvm.reviewboard.http.endpoints

import zio.*
import sttp.tapir.*
import sttp.client3.*
import sttp.tapir.json.zio.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.schemaForCaseClass // imports the type class derivation functionallity (to create the JsonCodecs)

import com.rockthejvm.reviewboard.domain.errors.HttpError
import com.rockthejvm.reviewboard.http.requests.LLMCallRequest
import com.rockthejvm.reviewboard.http.responses.LLMCallResponse

trait SummarizationEndpoints extends BaseEndpoint {

  val llmCallEndpoint =
      securedBaseEndpoint
        // OpenAI API endpoint for chat completions
        .in("v1" / "chat" / "completions")
        .post
        .in(jsonBody[LLMCallRequest])
        .out(jsonBody[LLMCallResponse])

}

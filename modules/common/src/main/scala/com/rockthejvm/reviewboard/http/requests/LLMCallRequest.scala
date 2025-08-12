package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

case class Message(content: String, role: String = "user") derives JsonCodec

final case class LLMCallRequest(
  model: String = "gpt-4.1",
  messages: List[Message]
  ) derives JsonCodec

object LLMCallRequest {
  def single(prompt: String): LLMCallRequest = LLMCallRequest(messages=List(Message(prompt)))
}

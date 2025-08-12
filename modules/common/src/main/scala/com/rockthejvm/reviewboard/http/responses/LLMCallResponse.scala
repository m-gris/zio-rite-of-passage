package com.rockthejvm.reviewboard.http.responses

import zio.json.JsonCodec

case class Message(
  content: String,
  role: String
  ) derives  JsonCodec

case class Chunk(
  index: Int,
  message: Message
  ) derives JsonCodec

final case class LLMCallResponse(
  id: String,
  created: Long,
  choices: List[Chunk]
  ) derives JsonCodec

package com.rockthejvm

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

case class Job(
  id: Long,
  title: String,
  url: String,
  company: String
  )

object Job:
  given codec: JsonCodec[Job] = DeriveJsonCodec.gen[Job] // macro-based JSON codec


case class CreateJobRequest(
  title: String,
  url: String,
  company: String,
  )


object CreateJobRequest:
  given codec: JsonCodec[CreateJobRequest] = DeriveJsonCodec.gen[CreateJobRequest] // macro-based JSON codec

package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class UserSession(
  email: String,
  token: String,
  expires: Long, // UNIX-Time in Seconds
  ) derives JsonCodec

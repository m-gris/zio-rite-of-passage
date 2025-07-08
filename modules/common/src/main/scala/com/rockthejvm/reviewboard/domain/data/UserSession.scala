package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class UserSession(
  id: Long,
  email: String,
  token: String,
  expires: Long, // UNIX-Time in Seconds
  ) derives JsonCodec

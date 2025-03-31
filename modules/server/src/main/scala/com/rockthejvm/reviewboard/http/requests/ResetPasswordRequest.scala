package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class ResetPasswordRequest(email: String, OTP: String, newPassword: String)
  derives JsonCodec

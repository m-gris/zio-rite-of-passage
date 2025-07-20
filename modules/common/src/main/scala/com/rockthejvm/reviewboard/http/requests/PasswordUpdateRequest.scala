package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class PasswordUpdateRequest(
    email: String,
    oldPassword: String,
    newPassword: String
) derives JsonCodec

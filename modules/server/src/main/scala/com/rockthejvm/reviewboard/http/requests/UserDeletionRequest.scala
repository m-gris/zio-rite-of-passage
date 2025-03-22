package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class UserDeletionRequest(email: String, password: String) derives JsonCodec

package com.rockthejvm.reviewboard.http.responses

import zio.json.JsonCodec

/*
 * As the User data structure becomes more complex
 * this response will allow us to "control" / "tailor" what we want to 'surface out'
 */
final case class UserResponse(email: String) derives JsonCodec

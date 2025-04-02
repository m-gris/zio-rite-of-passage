package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import sttp.tapir.generic.auto._
import com.rockthejvm.reviewboard.domain.data.User


final case class UserRegistration(
  email: String,
  password: String
  ){
    def toUser(id: Long) = ??? // User(id=id, email=email, hashedPassword=hashedPassword)
  }

object UserRegistration {
  given codec: JsonCodec[UserRegistration] = DeriveJsonCodec.gen[UserRegistration]
}

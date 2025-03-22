package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.config.JWTConfig

object JWTServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = 

    val userID = -1L
    val userEmail = "daniel@rockthejvm.com"

    suite("JWTServiceSpec")(

      test("start user session & validate token") {
        for {
          jwtService      <- ZIO.service[JWTService]
          userSession     <- jwtService.startSession(User(userID, userEmail, "unimportant"))
          identifiers     <- jwtService.verifyToken(userSession.token)
        } yield assertTrue(
            identifiers.id == userID &&
            identifiers.email == userEmail
        )
      }

      ).provide(
        JWTServiceLive.layer,
        ZLayer.succeed(JWTConfig("secret", 3600))
        )

}

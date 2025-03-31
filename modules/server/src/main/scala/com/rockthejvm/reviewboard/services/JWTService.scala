package com.rockthejvm.reviewboard.services

import java.time.Instant

import zio.*
import com.auth0.jwt.*
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWTVerifier.BaseVerification

import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.domain.data.*


trait JWTService {
  def startSession(user: User): Task[UserSession]
  def verifyToken(token: String): Task[Identifiers]
}


class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {

  private val ISSUER = "rockthejvm.com"
  private val CLAIM_USERNAME = "username"
  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)


  private val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .asInstanceOf[BaseVerification]
        .build(clock)


  override def startSession(user: User): Task[UserSession] = for {
      now        <- ZIO.attempt(clock.instant())// SIDE EFFECT
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
      jwt        <- ZIO.attempt(
        JWT
          .create()
          .withIssuer(ISSUER)
          .withIssuedAt(now)
          .withExpiresAt(expiration)
          .withSubject(user.id.toString) // the persons that tries to authenticate
          .withClaim(CLAIM_USERNAME, user.email)
          // once built... the JWT must be SIGNED & HASHED
          .sign(algorithm)
        )
  } yield UserSession(user.email, jwt, expiration.getEpochSecond())

  override def verifyToken(token: String): Task[Identifiers] = 
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId  <- ZIO.attempt(
        Identifiers(
          id = decoded.getSubject().toLong,
          email = decoded.getClaim(CLAIM_USERNAME).asString()
          )
        )
  } yield userId

}


object JWTServiceLive {
  val layer = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer = 
    Configs.makeLayer[JWTConfig]("rockthejvm.jwt") >>> layer

}

object JWTServiceDemo extends ZIOAppDefault {

  val program = for {
    service <- ZIO.service[JWTService]
    userSession   <- service.startSession(User(1L, "daniel@rockthejvm", "unimportant"))
    _       <- Console.printLine(userSession)
    userId  <- service.verifyToken(userSession.token)
    _       <- Console.printLine(userId.toString)
  } yield ()


  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(
      JWTServiceLive.layer,
      Configs.makeLayer[JWTConfig]("rockthejvm.jwt")
      )

}

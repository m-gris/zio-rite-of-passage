package com.rockthejvm.reviewboard.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.domain.data.UserSession


trait OTPRepository {

  def getOTP(email: String): Task[Option[String]]// option because the email might not exist in our db
  def checkOTP(email: String, OTP: String): Task[Boolean]
}

class OTPRepositoryLive private (
  quill: Quill.Postgres[SnakeCase],
  userRepo: UserRepository,
  config: OTPConfig,
) extends OTPRepository {

  import quill.*

  inline given schema: SchemaMeta[UserSession] =
    schemaMeta[UserSession](/* table name */"otps")

  inline given insMeta: InsertMeta[UserSession] =
    insertMeta[UserSession](/*empty... no col to exclude*/)

  inline given upMeta: UpdateMeta[UserSession] =
    updateMeta[UserSession](/* col to ignore */_.email)


  private val otpDuration = 600000 // millis ... TODO pass this from config

  private def randomUppercaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)


  inline private def selectUserWhere(inline email: String) =
    quote(query[UserSession].filter(u => u.email == lift(email)))

  private def findOTP(email: String): Task[Option[String]] =
    for {
      result <- run(selectUserWhere(email))
      _ <- ZIO.logDebug(s"Query result: $result")
      tokenOpt = result.headOption.map(_.token)
      _ <- ZIO.logDebug(s"Token option: $tokenOpt")
    } yield tokenOpt


  private def replaceOTP(email: String): Task[String] =
    for {
      // gen. otp
      otp <- randomUppercaseString(8)
      expiration = java.lang.System.currentTimeMillis() + otpDuration
      session = UserSession(email, otp, expiration)
      // run query
      _   <- run(
                selectUserWhere(email)
                .updateValue(lift(session))
                .returning(updatedRecord => updatedRecord)
              )
    } yield otp

  private def generateOTP(email: String): Task[String] =
    for {
      // gen. otp
      otp <- randomUppercaseString(8)
      expiration = java.lang.System.currentTimeMillis() + otpDuration
      session = UserSession(email, otp, expiration)
      // run query
      _   <- run(
                query[UserSession]
                .insertValue(lift(session))
                .returning(updatedRecord => updatedRecord)
              )
    } yield otp


  private def newOTP(email: String): Task[String] =
    findOTP(email).flatMap {
      case Some(_) => replaceOTP(email)
      case None    => generateOTP(email)
    }

  override def getOTP(email: String): Task[Option[String]] =
    // check user in db
    userRepo.getByEmail(email).flatMap {
      case None => ZIO.none
      case Some(_) => newOTP(email).option
    }

  override def checkOTP(email: String, otp: String): Task[Boolean] =

    for {
      now <- Clock.instant
      isValid <-     run(
                      selectUserWhere(email)
                        .filter( (userSession: UserSession) => userSession.token == lift(otp) && userSession.expires > lift(now.toEpochMilli) ) // List[UserSession]
                        .nonEmpty // Boolean
                      )
    } yield isValid

}


object OTPRepositoryLive {

  val layer = ZLayer {
    for {
      quill  <- ZIO.service[Quill.Postgres[SnakeCase]]
      repo   <- ZIO.service[UserRepository]
      config <- ZIO.service[OTPConfig]
    } yield new OTPRepositoryLive(quill, repo, config)
  }

  val configuredLayer =
    Configs.makeLayer[OTPConfig]("rockthejvm.otp") >>> layer


}

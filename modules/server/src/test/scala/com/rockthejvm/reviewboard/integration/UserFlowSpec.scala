package com.rockthejvm.reviewboard.integration

import zio.*
import zio.test.*
import zio.json.*
import sttp.client3.*

import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.ztapir.RIOMonadError
import sttp.tapir.server.ServerEndpoint
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.domain.data.UserSession
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.http.controllers.UserController


object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {
  // http controller
  // service
  // repository
  // database (test-container)

  override val initScript = "sql/integration.sql"

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  /*
   * REQ: a SYNCHRONOUS http server
   * allowing to send http requests as args to funcs
   * and getting back an http response SYNCHRONOUSLY
   */
  private def backendStubZIO =
    for {
      // 1. CREATE THE CONTROLLER
      controller <- UserController.makeZIO // requires an "injected" ReviewService
      // 2. Build TAPIR BACKEND (wrapped in a ZIO Effect)
      backendStub <- ZIO.succeed(
            /* the TapirStubInterpreter will run the controller's logic to those endpoints*/
            TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
              .whenServerEndpointsRunLogic(controller.routes)
              .backend()
          )
     /*will SYNCHRONOUSLY return http responses
      * as http requests are being pushed to that backendStub*/
    } yield backendStub


  def parse[T: JsonDecoder](
    response: Response[Either[String, String]]
  ): Either[String, T] = {
    response.body.flatMap(payload => payload.fromJson[T])
  }


  extension (backend: SttpBackend[ /*the interface derived both by: - SttpBackendStub - and our "real app server"*/
                          Task, // the EFFECT type (cats, zio ...)
                          Nothing // the CAPABILITY type (websockets, streams...)
                          ]) {

    def send[A: JsonCodec]( // [A: JsonCode] => `Context Bound` A must have a JsonCode typeclass in scope
      payload: A,
      method: Method,
      path: String,
      maybeToken: Option[String] = None
      ) = basicRequest
          .method(method, uri"$path")
          .body(payload.toJson) //serialization, extension method enabled tapir.generic_auto)
          .auth.bearer(maybeToken.getOrElse(""))
          .send(backend)

    def sendAndParse[A: JsonCodec, B: JsonCodec]( // [A: JsonCode] => `Context Bound` A must have a JsonCode typeclass in scope
      payload: A,
      method: Method,
      path: String,
      maybeToken: Option[String] = None
      ): Task[Either[String, B]] =
        send(payload, method, path, maybeToken)
          .map(parse[B])

    }


  class EmailServiceProbe extends EmailService {

    val db = collection.mutable.Map[String, String]()

    // will not really sent any email
    override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
      ZIO.unit

    // instead of sending... just store the otp create    // instead of sending... just store the otp create
    override def sendRecoveryEmail(to: String, otp: String): Task[Unit] =
      ZIO.succeed(db += (to -> otp))

    def probe(email: String): Task[Option[String]] =
      ZIO.succeed(db.get(email))

  }

  // will be used to "probe", i.e check that OTP was indeed sent...
  val EmailServiceLayer: ZLayer[Any, Nothing, EmailServiceProbe] =
    ZLayer.succeed(new EmailServiceProbe())

  override def spec: Spec[TestEnvironment & Scope, Any] =

    val USER_PWD = "12345"

    val USER_EMAIL = "joe@gmail.com"

    suite("UserFlowSpec")(

      test("register user") {
        for {

          backend <- backendStubZIO

          response <- backend
                        .send(
                          UserRegistration(USER_EMAIL, USER_PWD),
                          Method.POST,
                          "/users")
                        .map(parse[UserResponse])

        } yield assertTrue(response.contains(UserResponse(USER_EMAIL)))
      },

      test("register then login") {
        for {

          backend   <- backendStubZIO

          response <- backend
                        .sendAndParse[UserRegistration, UserResponse](
                          UserRegistration(USER_EMAIL, USER_PWD),
                          Method.POST,
                          "/users")

          session  <- backend
                        .sendAndParse[UserLoginRequest, UserSession](
                            UserLoginRequest(USER_EMAIL, USER_PWD),
                            Method.POST,
                            "/users/login")

        } yield assertTrue(
                  session.exists(_.email == USER_EMAIL)
                )
      },


      test("Change password") {
        for {

          backend   <- backendStubZIO

          response <- backend
                        .sendAndParse[UserRegistration, UserResponse](
                          UserRegistration(USER_EMAIL, USER_PWD),
                          Method.POST,
                          "/users")

          session  <- backend
                      .sendAndParse[UserLoginRequest, UserSession](
                        UserLoginRequest(USER_EMAIL, USER_PWD),
                        Method.POST,
                        "/users/login")
                          // .someOrFail(new RuntimeException("Authentication Failed"))

          _        <- backend
                        .sendAndParse[PasswordUpdateRequest, UserSession](
                          PasswordUpdateRequest(USER_EMAIL, USER_PWD, "newpwd123"),
                          Method.PUT,
                          "/users/password",
                          session.toOption.map(_.token)
                        )

          failedLogin   <- backend
                      .sendAndParse[UserLoginRequest, UserSession](
                        UserLoginRequest(USER_EMAIL, USER_PWD),
                        Method.POST,
                        "/users/login")



          succesfulLogin   <- backend
                      .sendAndParse[UserLoginRequest, UserSession](
                        UserLoginRequest(USER_EMAIL, "newpwd123"),
                        Method.POST,
                        "/users/login")


        } yield assertTrue(
                  failedLogin.isLeft
                  && succesfulLogin.isRight)
      },


      test("delete user") {
        for {

          backend   <- backendStubZIO

          // showcasing the "inspectability" of those layered integration tests
          // surfacing out the repo
          userRepo  <- ZIO.service[UserRepository]

          userRegistration <- backend
                        .sendAndParse[UserRegistration, UserResponse](
                          UserRegistration(USER_EMAIL, USER_PWD),
                          Method.POST,
                          "/users")

          // ex: inspecting the database directly...
          maybeOldUser <- userRepo.getByEmail(USER_EMAIL)

          userSession  <- backend
                        .sendAndParse[UserLoginRequest, UserSession](
                            UserLoginRequest(USER_EMAIL, USER_PWD),
                            Method.POST,
                            "/users/login")

          userDeletion  <- backend
                          .sendAndParse[UserDeletionRequest, UserSession](
                              UserDeletionRequest(USER_EMAIL, USER_PWD),
                              Method.DELETE,
                              "/users",
                              userSession.toOption.map(_.token)
                              )
          sessionAttempt <- backend
                              .sendAndParse[UserLoginRequest, UserSession](
                                  UserLoginRequest(USER_EMAIL, USER_PWD),
                                  Method.POST,
                                  "/users/login")


        } yield assertTrue(
                  maybeOldUser.exists(_.email == USER_EMAIL) &&
                  sessionAttempt.isLeft)

      },

      test("password reset flow") {

        val NEW_PWD = "newpwd123"

        for  {

          backend           <- backendStubZIO

          userRegistration  <- backend.send(
                                  UserRegistration(USER_EMAIL, USER_PWD),
                                  Method.POST,
                                  "/users")

          _                 <- backend.send(
                                  ForgotPasswordRequest(USER_EMAIL),
                                  Method.POST,
                                  "/users/forgot"
                                )

          emailService <- ZIO.service[EmailServiceProbe]


          otp           <- emailService.probe(USER_EMAIL)
                              .someOrFail(new RuntimeException("OTP was NOT emailed"))

          _                 <- backend.send(
                                  ResetPasswordRequest(USER_EMAIL, otp, NEW_PWD),
                                  Method.POST,
                                  "/users/reset"
                                )

          failedLogin   <- backend
                      .sendAndParse[UserLoginRequest, UserSession](
                        UserLoginRequest(USER_EMAIL, USER_PWD),
                        Method.POST,
                        "/users/login")



          succesfulLogin   <- backend
                      .sendAndParse[UserLoginRequest, UserSession](
                        UserLoginRequest(USER_EMAIL, NEW_PWD),
                        Method.POST,
                        "/users/login")


        } yield assertTrue(
                  failedLogin.isLeft
                  && succesfulLogin.isRight)

      },

      ).provide(
        EmailServiceLayer,
        UserServiceLive.layer,
        OTPRepositoryLive.configuredLayer,
        JWTServiceLive.configuredLayer,
        UserRepositoryLive.layer,
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        dataSourceLayer,
        Scope.default
        )

}


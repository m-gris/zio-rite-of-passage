package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.*
import sttp.tapir.server.*

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.domain.errors.*
import com.rockthejvm.reviewboard.services.JWTService
import com.rockthejvm.reviewboard.services.UserService
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import com.rockthejvm.reviewboard.http.responses.UserResponse

final case class UserController private (userService: UserService, jwtService: JWTService) extends BaseController with UserEndpoints {
  val register: ServerEndpoint[Any, Task] = userRegistrationEndpoint
    .serverLogic { request =>
      userService
        .registerUser(request.email, request.password)
        .map(user => UserResponse(user.email))
        .either
  }

  val login: ServerEndpoint[Any, Task] = userLoginEndpoint
    .serverLogic {request =>
      userService
        .login(request.email, request.password)
        .someOrFail(UnauthorizedException)
        .either
      }

  val updatePassword: ServerEndpoint[Any, Task] = passwordUpdateEndpoint
        // SECURITY LOGIC -- before ServerLogic can Run...
        .serverSecurityLogic[Identifiers, Task](token => jwtService.verifyToken(token).either)
        .serverLogic { // given the security, the serverLogic signature is now
          userIdentifiers => request => /* curried function */
            userService
              .updatePassword(request.email, request.oldPassword, request.newPassword)
              .map(user => UserResponse(user.email))
              .either
        }

  val deleteAccount: ServerEndpoint[Any, Task] =
    userDeletionEndpoint
      .serverSecurityLogic[Identifiers, Task](token => jwtService.verifyToken(token).either)
      .serverLogic {identifiers => request =>
        userService
          .deleteUser(request.email, request.password)
          .map(user => UserResponse(user.email))
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(register, login, updatePassword, deleteAccount)

}


object UserController {
  /* EFFECTFUL 'smart constructor' */
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield UserController(userService, jwtService)

}

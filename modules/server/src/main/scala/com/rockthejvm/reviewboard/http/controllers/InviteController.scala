package com.rockthejvm.reviewboard.http.controllers

import zio.*
import zio.Task
import com.rockthejvm.reviewboard.http.endpoints.InviteEndpoints
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.services.JWTService
import com.rockthejvm.reviewboard.domain.data.Identifiers
import com.rockthejvm.reviewboard.http.requests.InvitePackRequest
import com.rockthejvm.reviewboard.services.InviteService
import com.rockthejvm.reviewboard.http.responses.InviteResponse

class InviteController private (inviteService: InviteService, jwtService: JWTService)
    extends BaseController
    with InviteEndpoints {

  val addPack =
    addPackEndpoint
      .serverSecurityLogic[Identifiers, Task](identifiers =>
        jwtService.verifyToken(identifiers).either
      )
      .serverLogic { (identifiers: Identifiers) => (request: InvitePackRequest) =>
        inviteService
          .addInvitePack(identifiers.email, request.companyId) // Task[Long]
          .map(_.toString)                                     // Task[String]
          .either                                              // Either[Throwable, String]
      }

  val invite =
    inviteEndpoint
      .serverSecurityLogic[Identifiers, Task](identifiers =>
        jwtService.verifyToken(identifiers).either
      )
      .serverLogic { identifiers => request =>
        inviteService
          .sendInvites(identifiers.email, request.companyId, request.emails)
          .map { nInvitesSent =>
            if nInvitesSent == request.emails.size
            then InviteResponse("OK", nInvitesSent)
            else InviteResponse("Partial Success", nInvitesSent)
          }
          .either
      }

  val getByUserName =
    getByUserNameEndpoint
      .serverSecurityLogic[Identifiers, Task](identifiers =>
        jwtService.verifyToken(identifiers).either
      )
      .serverLogic { identifiers => _ /* the input is Unit */ =>
        inviteService.getByUserName(identifiers.email).either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    addPack,
    invite,
    getByUserName
  )

}

object InviteController {
  def makeZIO = for {
    inviteService <- ZIO.service[InviteService]
    jwtService    <- ZIO.service[JWTService]
  } yield new InviteController(inviteService, jwtService)
}

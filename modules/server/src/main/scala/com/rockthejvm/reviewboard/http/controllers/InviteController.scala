package com.rockthejvm.reviewboard.http.controllers

import zio.*
import zio.Task
import com.rockthejvm.reviewboard.http.endpoints.InviteEndpoints
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.services.JWTService
import com.rockthejvm.reviewboard.domain.data.Identifiers
import com.rockthejvm.reviewboard.http.requests.InvitePackRequest
import com.rockthejvm.reviewboard.services.InviteService
import com.rockthejvm.reviewboard.http.responses.InviteResponse
import com.rockthejvm.reviewboard.services.PaymentService

class InviteController private (
    inviteService: InviteService,
    jwtService: JWTService,
    paymentService: PaymentService
) extends BaseController
    with InviteEndpoints {

  val addPack: ServerEndpoint[Any, Task] =
    addPackEndpoint
      .serverSecurityLogic[Identifiers, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { (identifiers: Identifiers) => (request: InvitePackRequest) =>
        inviteService
          .addInvitePack(identifiers.email, request.companyId) // Task[Long]
          .map(_.toString)                                     // Task[String]
          .either                                              // Either[Throwable, String]
      }

  val invite: ServerEndpoint[Any, Task] =
    inviteEndpoint
      .serverSecurityLogic[Identifiers, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { identifiers => request =>
        inviteService
          .sendInvites(identifiers.email, request.companyId, request.emails)
          .map { nInvitesSent =>
            InviteResponse("OK", nInvitesSent)
          }
          .either
      }

  val getByUserName: ServerEndpoint[Any, Task] =
    getByUserNameEndpoint
      .serverSecurityLogic[Identifiers, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { identifiers => _ /* the input is Unit */ =>
        inviteService.getByUserName(identifiers.email).either
      }

  val addPackPromote: ServerEndpoint[Any, Task] = addPackPromotedEndpoint
    .serverSecurityLogic[Identifiers, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { identifiers => req =>
      inviteService
        .addInvitePack(identifiers.email, req.companyId)
        .flatMap { packId =>
          paymentService.createCheckoutSession(packId, identifiers.email)
        } // at that point we have an Option[StripeSession] wrapped in a Zio Effect
        .someOrFail(new RuntimeException("Cannot create checkout session"))
        .map(checkoutSession => checkoutSession.getUrl) // the DESIRED PAYLOAD
        .either
    }

  val webhook: ServerEndpoint[Any, Task] =
    webhookEndpoint
      .serverLogic { (signature: String, payload: String) =>
        paymentService.handleWebhookEvent(
          signature,
          payload,
          packId => inviteService.activatePack(packId.toLong)
          )
          .unit // discard the boolean value
          .either

      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    webhook, // MOVE HERE... TENTATIVELY...
    addPack,
    invite,
    getByUserName,
    addPackPromote,
  )

}

object InviteController {
  def makeZIO = for {
    inviteService  <- ZIO.service[InviteService]
    jwtService     <- ZIO.service[JWTService]
    paymentService <- ZIO.service[PaymentService]
  } yield new InviteController(inviteService, jwtService, paymentService)
}

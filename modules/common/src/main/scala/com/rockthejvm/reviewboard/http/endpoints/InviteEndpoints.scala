package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation

import com.rockthejvm.reviewboard.domain.data.Invitations
import com.rockthejvm.reviewboard.http.responses.InviteResponse
import com.rockthejvm.reviewboard.http.requests.{InvitePackRequest, InviteRequest}

trait InviteEndpoints extends BaseEndpoint {

  /*POST / invite / add
   *
   * input { companyId }
   *
   * output packId as a string
   * */
  val addPackEndpoint =
    securedBaseEndpoint
      .tag("Invite")
      .name("Add invitees")
      .description("Get invite tokens")
      .in("invite" / "add")
      .post
      .in(jsonBody[InvitePackRequest])
      .out(stringBody)

  /** POST /invite
    *
    * input { [emails], companyId }
    *
    * output { nInvites, status }
    *
    * will send emails to users
    */
  val inviteEndpoint =
    securedBaseEndpoint
      .tag("invites")
      .name("invite")
      .description("Send people emails, inviting them to leave a review.")
      .in("invite")
      .post
      .in(jsonBody[InviteRequest])
      .out(jsonBody[InviteResponse])

  /** GET /invite/all
    *
    * output [ { companyId, companyName, nInvites } ]
    */
  val getByUserNameEndpoint =
    securedBaseEndpoint
      .tag("invites")
      .name("get by user ID")
      .description("Get all active invite packs for a user")
      .in("invite" / "all")
      .get
      .out(jsonBody[List[Invitations]])

  /** GET /invite/all
    *
    * output [ { companyId, companyName, nInvites } ]
    */

// TODO - paid endpoints

// TODO - paid endpoints

}

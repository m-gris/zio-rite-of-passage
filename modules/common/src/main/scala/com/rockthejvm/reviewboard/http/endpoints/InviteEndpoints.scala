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
      .tag("invites")
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

  /*POST / invite / promoted
   *
   * input { companyId }
   *
   * output packId as a string
   * */
  val addPackPromotedEndpoint =
    securedBaseEndpoint
      .tag("invites")
      .name("Add invitees (promoted)")
      .description("Get invite tokens (via Stripe)")
      .in("invite" / "promoted")
      .post
      .in(jsonBody[InvitePackRequest])
      .out(stringBody) // the STRIPE CHECKOUT URL

  // WEBHOOK
  // a plain endpoint
  // that will be called automatically
  // by the service on which this webhook is registered
  // (in our case Stripe)
  // Said differenty
  // we will REGISTER THIS WEBHOOK at Stripe
  // Stripe will call this webhook after CHECKOUT COMPLETION
  // The webhook will contain multiple(?) payloads, with information such as:
  //  - completion success
  //  - if a tax invoice must be sent
  //  - etc ...
  //
  // hit /invite/promoted -> add a new (inactive) email pack + return Stripe Checkout URL,
  // go to the URL, fill in the details, hit Pay
  // after a while, Stripe will call the webhook -> only then shall we activate the pack
  //
  val webhookEndpoint =
    baseEndpoint
      .tag("invites")
      .name("invite webhook")
      .description("confirms the purchase of an invite pack")
      .in("invite"/"webhook")
      .post
      .in(header[String]("Stripe-Signature"))
      .in(stringBody) // will be parsed into a webhook-event
      // side note: 2 `.in()` ... multiple inputs... the (String, String) in
      // val webhookEndpoint: Endpoint[Unit, (String, String), Throwable, Unit, Any]

}

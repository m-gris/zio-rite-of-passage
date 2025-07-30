package com.rockthejvm.reviewboard.services

import zio.*
import com.stripe.Stripe as TheStripe
// import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional
import com.stripe.model.checkout.{Session => StripeSession}
import com.stripe.param.checkout.SessionCreateParams
import com.rockthejvm.reviewboard.config.PaymentConfig
import com.rockthejvm.reviewboard.config.Configs
import org.testcontainers.shaded.org.bouncycastle.crypto.tls.SessionParameters
import com.stripe.net.Webhook
import java.util.Optional
import com.stripe.model.StripeObject

trait PaymentService {

  // Create a session
  def createCheckoutSession(packId: Long, userName: String): Task[Option[StripeSession]]

  // Handle a WebHook Event
  def handleWebhookEvent[A](
    signature: String,
    payload: String,
    action: String => Task[A]
  ): Task[Option[A]]

}

class PaymentServiceLive(config: PaymentConfig) extends PaymentService {

  override def createCheckoutSession(packId: Long, userName: String): Task[Option[StripeSession]] =
    ZIO.logWarning(s"Creating checkout session for packId: $packId, user: $userName") *>
    ZIO
      .attempt {

        // BIG / CLUNKY JAVA API
        SessionCreateParams
          .builder()
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setSuccessUrl(config.successUrl)
          .setCancelUrl(config.cancelUrl)
          .setCustomerEmail(userName)
          .setClientReferenceId(packId.toString) // our custom payload for the WEBHOOK
          .setInvoiceCreation(
            SessionCreateParams.InvoiceCreation
              .builder()
              .setEnabled(true)
              .build()
          )
          .setPaymentIntentData(
            SessionCreateParams.PaymentIntentData
              .builder()
              .setReceiptEmail(userName)
              .build()
          )
          .addLineItem(
            SessionCreateParams.LineItem
              .builder()
              .setPrice(
                config.price
              ) // unique id of the stripe product (must be registered at stripe)
              .setQuantity(1L)
              .build()
          )
          .build()
      }
      .map(params => StripeSession.create(params))
      .tap(session => ZIO.logWarning(s"Created Stripe session with ID: ${session.getId}, clientReferenceId: ${session.getClientReferenceId}"))
      .map(Option(_))

  override def handleWebhookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
    ): Task[Option[A]] = ZIO.attempt {
      //build webhook event
      Webhook.constructEvent(payload, signature, config.secret)
    }.flatMap { event =>
      // check event type
      ZIO.logInfo(s"### *** ### event: $event ### *** ###")
      event.getType() match {
        case "checkout.session.completed" =>
          ZIO.attempt {
            val deserializer = event.getDataObjectDeserializer()
            val session = deserializer.deserializeUnsafe().asInstanceOf[StripeSession]
            Option(session.getClientReferenceId)
          }.flatMap {
            case Some(refId) =>
              action(refId).map(Some(_))
            case None =>
              ZIO.fail(new RuntimeException("No client reference ID found in checkout session"))
          }
        case _ => ZIO.none
      }
    }

}


object PaymentServiceLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[PaymentConfig]
      _ <- ZIO.attempt(TheStripe.apiKey =
        config.apiKey
      ) // Stripe Java SDK forces global API key mutation. Wrapped in ZIO for:
        // - Error handling during initialization (invalid key format, etc.)
        // - One-time setup in layer vs repeated assignment per request
        // - Consistency with ZIO's effect system for side effects
    } yield new PaymentServiceLive(config)
  }

  val configuredLayer =
    Configs.makeLayer[PaymentConfig]("rockthejvm.payment") >>> layer
}

package com.rockthejvm.reviewboard.services

import com.stripe.model.checkout.* // note SessionCreateParam from package checkout
import com.stripe.Stripe as TheStripe
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams

import scala.jdk.OptionConverters.* // to convert JAVA Optional => SCALA Option

import zio.*

import com.rockthejvm.reviewboard.config.{Configs, StripeConfig}

// own imports to pin webhook API version
import com.stripe.param.WebhookEndpointCreateParams
import java.util.Arrays
import com.stripe.model.WebhookEndpoint

trait PaymentService {
  // create a session (Session is a type from Stripe)
  def createCheckoutSession(invitePackId: Long, userName: String): Task[Option[Session]]
  // handle a webhook event
  def handleWebHookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
  ): Task[Option[A]]
}

class PaymentServiceLive private (config: StripeConfig) extends PaymentService {
  def createCheckoutSession(invitePackId: Long, userName: String): Task[Option[Session]] =
    ZIO
      .attempt {
        SessionCreateParams
          .builder()
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setSuccessUrl(config.successUrl)
          .setCancelUrl(config.cancelUrl)
          .setCustomerEmail(userName)
          .setClientReferenceId(invitePackId.toString)
          // ^ my own payload - will be used on the webhook
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
          // need to add a product
          .addLineItem(
            SessionCreateParams.LineItem
              .builder()
              .setPrice(config.price) // unique id of your Stripe product
              .setQuantity(1L)
              .build()
          )
          .build()
      }
      .map(params => Session.create(params))
      .map(Option(_))
      .logError("Stripe session creation FAILED")
      .catchSome { case _ =>
        ZIO.none
      }

  override def handleWebHookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
      // this HOF added as its not a good idea for Payment Service to depend on inviteService and use it
  ): Task[Option[A]] = {
    ZIO
      .attempt {
        Webhook.constructEvent(payload, signature, config.secret)
      }
      .flatMap { event =>
        event.getType() match {
          case "checkout.session.completed" =>
            Console.printLine(
              event
                .getDataObjectDeserializer()
                .getObject()
                // .toScala
                // .map(
                //   _.asInstanceOf[Session]
                // ) // java optional -> scala option -> downcasr StripeObject -> StripeSession
                // .map(_.getClientReferenceId())
            ) *>
              ZIO.foreach(
                event
                  .getDataObjectDeserializer()
                  .getObject()
                  .toScala
                  .map(
                    _.asInstanceOf[Session]
                  ) // java optional -> scala option -> downcasr StripeObject -> StripeSession
                  .map(_.getClientReferenceId())
              )(action) // activate (action(packId))
          case _ =>
            Console.printLine("didnt work") *>
              ZIO.none
          // discard the event
        }

      }
  }
  /*
        build webhoook event
        check event type
          if event type is sucess, handle the activation of the pack
            parse the event
            handle the activation of the pack
   */

}

object PaymentServiceLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[StripeConfig]
      _      <- ZIO.attempt(TheStripe.apiKey = config.key) // API key wrapped in ZIO
      // _      <- setApiVersionWebHook // TODO : OWN
      // NOTE: api version pinned according to the build.sbt version , if not problems in deseriablization, you can check API version to replace here in stripe Overview -> API version,
      // by default stripe used the latest api version which results in a mismatch if not adjusted.
    } yield new PaymentServiceLive(config)
  }

  val configuredLayer =
    Configs.makeLayer[StripeConfig]("rockthejvm.stripe") >>> layer

  // OWN
  def setApiVersionWebHook = {
    ZIO.attempt {
      val params: WebhookEndpointCreateParams = {
        WebhookEndpointCreateParams
          .builder()
          .setUrl("http://localhost:8080/invite/webhook") // TODO : deploy garepachi need to add public URL here using config
          .addAllEnabledEvent(
            Arrays.asList(
              WebhookEndpointCreateParams.EnabledEvent.PAYMENT_INTENT__PAYMENT_FAILED,
              WebhookEndpointCreateParams.EnabledEvent.PAYMENT_INTENT__SUCCEEDED
            )
          )
          .setApiVersion(WebhookEndpointCreateParams.ApiVersion.VERSION_2024_10_28_ACACIA)
          .build()
      }
      WebhookEndpoint.create(params)
    }

  }
}

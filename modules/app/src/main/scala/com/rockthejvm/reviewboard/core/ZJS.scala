package com.rockthejvm.reviewboard.core

import zio.*

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import sttp.client3.* // needed for uri""
import sttp.model.Uri
import sttp.tapir.Endpoint

import com.rockthejvm.reviewboard.config.*
import java.awt.Event
import scala.annotation.targetName

object ZJS {

  /* This is replace by below as it does the same thing
    def backendCall[A](clientFun: BackendClient => Task[A]) =
        ZIO.serviceWithZIO[BackendClient](clientFun)
   */

  def useBackend =
    ZIO.serviceWithZIO[BackendClient] // retrieves the service but also immediately uses it once service provided to produce another ZIO effect

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafe { implicit unsafe => // run the ZIO effect
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }

    def toEventStream: EventStream[A] = {
      val bus = EventBus[A]()
      emitTo(bus)
      bus.events
    }

    def runJs = // created for Var[]() instead of EventBus , "unsafe.runToFuture()" blocking run method  - NOTE: removed .runToFuture() because its add extra things
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(zio.provide(BackendClientLive.configuredLayer))
      }
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .service[BackendClient]
        .flatMap(backendClient => backendClient.endpointRequestZIO(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)

  extension [I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  ) // String for secure endpoint
    @targetName(
      "applySecure"
    ) // because both extension have apply that will reduce to same in js so need to add target name(doesn't effect scala code just js)
    def apply(payload: I): Task[O] =
      ZIO
        .service[BackendClient]
        .flatMap(backendClient => backendClient.secureEndpointRequestZIO(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)
}

package com.rockthejvm.reviewboard.core

import zio.*
import sttp.client3.*
import sttp.tapir.Endpoint
import sttp.model.Uri.UriContext
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter

import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.config.BackendClientConfig

object ZJS {

    def useBackend = ZIO.serviceWithZIO[BackendClient]

    // NEED TO RUN THE ZIO EFFET 'MANUALLY' / 'UNSAFELY'
    // Because our frontend is not a ZIO Native APP (we use Laminar for the "logic")
    // ZIO will be used as an 'auxiliary' to fetch things from the backend
    // We will do so by:
    //  - running the effect
    //  - and surface its output as an event stream in laminar
    //  ( by pushing the effect's output to an event bus)
    extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
      def emitTo(eventBus: EventBus[A]) =
        Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.fork(
            zio
              .tap(value => ZIO.attempt(eventBus.emit(value)))
              .provide(BackendClientLive.configuredLayer)
          )
        }
    }

    extension [I, E <: Throwable, O](endpoint: Endpoint[Unit,I,E,O,Any])
      def apply(payload: I): Task[O] =
        ZIO
          .service[BackendClient]
          .flatMap(_.sendRequestZIO(endpoint)(payload))
          .provide(BackendClientLive.configuredLayer)


  }


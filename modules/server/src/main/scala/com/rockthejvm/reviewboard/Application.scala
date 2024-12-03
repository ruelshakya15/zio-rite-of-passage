package com.rockthejvm.reviewboard

import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor

import zio.*
import zio.http.Server
import zio.http.ServerConfig

import java.net.InetSocketAddress

import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.config.*


object Application extends ZIOAppDefault {

  // since ZIO http has default port hardcoded for Server.default we make our own Server layer to override it according to config
  val configuredServer =
    Configs.makeLayer[HttpConfig]("rockthejvm.http") >>>
    ZLayer(
      ZIO.service[HttpConfig]
      .map(config => ServerConfig.default.copy(address = InetSocketAddress(config.port)))
    ) >>> Server.live

  def runMigrations = for {
    flyway <- ZIO.service[FlywayService]
    _ <- flyway.runMigrations.catchSome{
      case e => ZIO.logError("MIGRATIONS FAILED: " + e) *>
        flyway.runRepairs *> flyway.runMigrations
    }
  } yield ()

  def startServer = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default.appendInterceptor(
          CORSInterceptor.default
        )
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  def program = for {
    _ <- ZIO.log("Rock the JVM! Bootstrapping...")
    _ <- runMigrations
    _ <- startServer
  } yield ()

  def run =
    program.provide(
      configuredServer,
      // services
      ReviewServiceLive.configuredLayer,
      CompanyServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.configuredLayer,
      EmailServiceLive.configuredLayer,
      InviteServiceLive.configuredLayer,
      PaymentServiceLive.configuredLayer,
      OpenAIServiceLive.configuredLayer,
      FlywayServicelive.configuredLayer,
      // repos
      ReviewRepositoryLive.layer,
      CompanyRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.configuredLayer,
      InviteRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}

/**   - ReviewEndpoints - get/set (summaries)
  *   - controller
  *   - ReviewService
  *     - get - fetch it from the repo
  *     - set - invoke the OpenAI API + store in the repo
  *   - repo
  *
  * Frontend
  *
  * -modify the current "TODO" card
  */

  /** BUG bashing part
    * - tests work
    * - reviews are set in the future (TZ difference)
    *   - Instant at writing
    *   - Instant at reading (on frontend)
    * - invalid pages 
    * - footer
    * - some images won't upload in the add company page
    */

package com.rockthejvm.reviewboard.http.controllers

import zio.*

import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import com.rockthejvm.reviewboard.domain.errors.HttpError

import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*

class HealthController private extends BaseController with HealthEndpoint {
  val health: ServerEndpoint[Any, Task] = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))

  val errorRoute: ServerEndpoint[Any, Task] = errorEndpoint
    .serverLogic[Task](_ =>
      ZIO
        .fail(new RuntimeException("Boom!"))
        .either // need to convert Task[String] => Task[Either[Throwable, String]] to surface out the error so we can extract the message or else compiler won't catch it
    )
  override val routes: List[ServerEndpoint[Any, Task]] = List(health, errorRoute)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}

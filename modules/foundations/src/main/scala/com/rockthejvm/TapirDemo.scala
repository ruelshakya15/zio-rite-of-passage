package com.rockthejvm

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server
import zio.json.{DeriveJsonCodec, JsonCodec}

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault {

  val simpleEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("simplest endpoint possible")
    // ^^ for documentation
    .get                    // http method
    .in("simple")           // path
    .out(plainBody[String]) // output
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))
  // from ZIO http.Server
  val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add config e.g CORS
    ).toHttp(simpleEndpoint)
  )

  // simulate a job board
  val db: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1L, "Instructor", "rockthejvm.com", "Rock the JVM")
  )

  // create
  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create")
    .description("Create a job")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest]) // here need an implicit which is made available by  (sttp.tapir.generic.auto.*)
    .out(jsonBody[Job])
    .serverLogicSuccess(req =>
      ZIO.succeed {
        // insert a new job in my "db"
        val newId  = db.keys.max + 1
        val newJob = Job(newId, req.title, req.url, req.company)
        db += (newId -> newJob)
        newJob
      }
    )

  // get by id
  val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getById")
    .description("Get job by id")
    .in("jobs" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Job]])
    .serverLogicSuccess(id => ZIO.succeed(db.get(id)))

  // get all
  val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getAll")
    .description("Get all jobs")
    .in("jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default
    ).toHttp(
      List(createEndpoint, getByIdEndpoint, getAllEndpoint)
    ) // toHttp require List of type ServerEndpoint[C, RIO[R, *]] => so we add  ServerEndpoint[Any, Task] type to endpoints
  )
  override def run = serverProgram.provide(
    Server.default // without other configs, should start at 0.0.0.0:8080
  )
}


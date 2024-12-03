package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.controllers.CompanyController
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.services.JWTService

import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError

import zio.*
import zio.json.*
import zio.test.*
import com.rockthejvm.reviewboard.domain.data.{User, UserID, UserToken}
import com.rockthejvm.reviewboard.domain.data.CompanyFilter

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any] // monad error with requirement Any

  private val rtjvm = Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")
  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(rtjvm)
    override def getById(id: Long): Task[Option[Company]] =
      ZIO.succeed {
        if (id == 1) Some(rtjvm)
        else None
      }
    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed {
        if (slug == rtjvm.slug) Some(rtjvm)
        else None
      }
    override def getAll: Task[List[Company]] =
      ZIO.succeed(List(rtjvm))

    override def allFilters: Task[CompanyFilter] =
      ZIO.succeed(CompanyFilter())

    override def search(filter: CompanyFilter): Task[List[Company]] =
      ZIO.succeed(List(rtjvm))
  }

  private val jwtServiceStub = new JWTService { // not actually used
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.id, user.email, "ALL_IS_GOOD", 99999999L))
    override def verifyToken(token: String): Task[UserID] =
      ZIO.succeed(UserID(1, "daniel@rockthejvm.com"))
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    // create the controller
    controller <- CompanyController.makeZIO
    // build tapir backend
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(
          endpointFun(controller)
        ) // *** IMP higher order function look at defn above of endpointFun too
        .backend()
    )
  } yield backendStub

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          // run http request
          response <- basicRequest // done through import sttp.client3.*
            .post(uri"/companies")
            .body(
              CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson
            ) // need to input json so we import (import zio.json.*, sttp.generic.auto.*)
            .header(
              "Authorization",
              "Bearer ALL_IS_GOOD"
            ) // token doesn't matter because verification is mocked
            .send(backendStub)
        } yield response.body // .body returns Either[String, String] only need Right one Left is for error

        // we would like simpler syntax like program.assert(_ == 2) USED extension method in syntax/AssertionSyntax folder to reduce boilerplate
        // inspect http response
        program.assert { respBody =>
          respBody.toOption // Option[Company]  // .toOption => Returns a Some containing the Right value if it exists or a None if this is a Left.
            .flatMap(
              _.fromJson[Company].toOption
            )
            .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
        }
      },
      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(rtjvm))
        }
      },
      test("get by id") {
        val program = for {
          controller  <- CompanyController.makeZIO
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(rtjvm)
        }
      }
    )
      .provide(
        ZLayer.succeed(serviceStub),
        ZLayer.succeed(jwtServiceStub) // not actually used
      )
}

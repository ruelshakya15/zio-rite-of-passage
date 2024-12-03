package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.services.ReviewService
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

import java.time.Instant
import com.rockthejvm.reviewboard.domain.data.{Review, User, UserID, UserToken}
import com.rockthejvm.reviewboard.domain.data.ReviewSummary

object ReviewControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any] // monad error with requirement Any

  private val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 10,
    review = "all good",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val serviceStub = new ReviewService {
    override def create(req: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] = ZIO.succeed {
      if (id == 1) Some(goodReview)
      else None
    }
    override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
      if (companyId == 1) List(goodReview)
      else List()
    }

    override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
      if (userId == 1) List(goodReview)
      else List()
    }

    override def getSummary(companyId: Long): Task[Option[ReviewSummary]] = 
      ZIO.none
    
    override def makeSummary(companyId: Long): Task[Option[ReviewSummary]] = 
      ZIO.none
    
  }

  private val jwtServiceStub = new JWTService { // not actually used
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.id, user.email, "ALL_IS_GOOD", 99999999L))
    override def verifyToken(token: String): Task[UserID] =
      ZIO.succeed(UserID(1, "daniel@rockthejvm.com"))
  }

  private def backendStubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) = for {
    // create the controller
    controller <- ReviewController.makeZIO
    // build tapir backend
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(
          endpointFun(controller)
        )
        .backend()
    )
  } yield backendStub

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewControllerSpec")(
      test("post review") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/reviews")
            .body(
              CreateReviewRequest(
                companyId = goodReview.companyId,
                management = goodReview.management,
                culture = goodReview.culture,
                salary = goodReview.salary,
                benefits = goodReview.benefits,
                wouldRecommend = goodReview.wouldRecommend,
                review = goodReview.review
              ).toJson
            )
            .header(
              "Authorization",
              "Bearer ALL_IS_GOOD"
            ) // token doesn't matter because verification is mocked
            .send(backendStub)
        } yield response.body

        program.assert {
          _.toOption                              // Option[String]
            .flatMap(_.fromJson[Review].toOption) // Option[Review]
            .contains(goodReview)
        }
      },
      test("get by id") {
        for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/reviews/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/999")
            .send(backendStub)

        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .isEmpty
        }
      },
      test("get by Company id") {
        for {
          backendStub <- backendStubZIO(_.getByCompanyId)
          response <- basicRequest
            .get(uri"/reviews/company/1")
            .send(backendStub) // returns List(goodReview)
          responseNotFound <- basicRequest
            .get(uri"/reviews/company/999")
            .send(backendStub) // returns List()

        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(goodReview)) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List())
        }
      }
    ).provide(
      ZLayer.succeed(serviceStub),
      ZLayer.succeed(jwtServiceStub) // not actually used
    )

}

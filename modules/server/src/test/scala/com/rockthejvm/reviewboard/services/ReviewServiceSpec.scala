package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*


import java.time.Instant

import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.config.SummaryConfig
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.services.CompanyServiceSpec.service
import com.rockthejvm.reviewboard.domain.data.ReviewSummary

// NOTE: unlike RepositorySpec we dont need to actually test if data is stored in DB so we make a temporal "Repository" to just check if
// "Service" layer works correctly

object ReviewServiceSpec extends ZIOSpecDefault {

  val goodReview = Review(
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

  val badReview = Review(
    id = 2L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salary = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "BAD BAD",
    created = Instant.now(),
    updated = Instant.now()
  )

  val stubRepoLayer = ZLayer.succeed {
    new ReviewRepository {
      override def create(review: Review): Task[Review] =
        ZIO.succeed(goodReview)
      override def getById(id: Long): Task[Option[Review]] =
        ZIO.succeed {
          id match {
            case 1 => Some(goodReview)
            case 2 => Some(badReview)
            case _ => None
          }
        }
      override def getByCompanyId(id: Long): Task[List[Review]] = ZIO.succeed {
        if (id == 1) List(goodReview, badReview)
        else List()
      }

      override def getByUserId(id: Long): Task[List[Review]] = ZIO.succeed {
        if (id == 1) List(goodReview, badReview)
        else List()
      }
      // not used but needed
      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id $id not found")).map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id $id not found"))

      override def getSummary(companyId: Long): Task[Option[ReviewSummary]] = 
        ZIO.none
      
      override def insertSummary(companyId: Long, summary: String): Task[ReviewSummary] =
        ZIO.succeed(ReviewSummary(companyId, summary, Instant.now()))
    }

  }

  val stubReviewSummaryService = ZLayer.succeed {
    new OpenAIService {
      override def getCompletion(prompt: String): Task[Option[String]] =
        ZIO.none
    }
  }

  val summaryConfigLayer = ZLayer.succeed {
    SummaryConfig(3, 20)
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewServiceSpec")(
      test("create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              companyId = goodReview.companyId,
              management = goodReview.management,
              culture = goodReview.culture,
              salary = goodReview.salary,
              benefits = goodReview.benefits,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = 1L
          )
        } yield assertTrue(
          review.companyId == goodReview.companyId &&
            review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salary == goodReview.salary &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        )
      },
      test("get by id") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getById(1L)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue(
          review.contains(goodReview) && reviewNotFound.isEmpty
        )
      },
      test("get by companyId") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByCompanyId(1L)
          reviewsNotFound <- service.getByCompanyId(999L)
        } yield assertTrue(
          reviews.toSet == Set(goodReview, badReview) &&
            reviewsNotFound.isEmpty
        )
      },
      test("get by userId") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByUserId(1L)
          reviewsNotFound <- service.getByUserId(999L)
        } yield assertTrue(
          reviews.toSet == Set(goodReview, badReview) &&
            reviewsNotFound.isEmpty
        )
      }
    ).provide(
      ReviewServiceLive.configuredLayer,
      stubRepoLayer,
      stubReviewSummaryService,
    )
}

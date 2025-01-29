package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.services.ReviewService
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.repositories.ReviewRepositorySpec.badReview
import com.rockthejvm.reviewboard.repositories.ReviewRepositorySpec.goodReview
import com.rockthejvm.reviewboard.http.requests.ReviewCreationRequest

object ReviewServiceSpec extends ZIOSpecDefault {

  // using the ReviewService requires stubbing the repository layer
  val stubRepoLayer = ZLayer.succeed {

    new ReviewRepository {

      override def create(review: Review): Task[Review] = ZIO.succeed(goodReview)

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"ID $id not found")).map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"ID $id not found"))

      override def getById(id: Long): Task[Option[Review]] = ZIO.succeed {
        id match {
          case goodReview.id => Some(goodReview)
          case badReview.id  => Some(badReview)
          case _             => None
        }
      }

      override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
        List(goodReview, badReview).filter(_.userId == userId)
      }

      override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
        List(goodReview, badReview).filter(_.companyId == companyId)
      }

    }
  }


  override def spec: Spec[TestEnvironment & Scope, Any] = {

    suite("ReviewServiceSpec")(

      test("create review") {
        for {
          service <- ZIO.service[ReviewService]
          review  <- service.create(
            request = ReviewCreationRequest(
              companyId = goodReview.companyId,
              management = goodReview.management,
              culture = goodReview.culture,
              salary = goodReview.salary,
              benefits = goodReview.benefits,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = goodReview.userId
          )
        } yield assertTrue { true }
      },

      test("get by ID") {
        for {
          service <- ZIO.service[ReviewService]
          review  <- service.getById(goodReview.id)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue {
            review.contains(goodReview) &&
            reviewNotFound.isEmpty
        }
      },

      test("get by userId") {
        for {
          service        <- ZIO.service[ReviewService]
          reviews        <- service.getByUserId(1L)
          reviewNotFound <- service.getByUserId(999L)
        } yield assertTrue {
            reviews.toSet  == Set(goodReview, badReview)
            reviewNotFound.isEmpty
        }
      },


      test("get by companyId") {
        for {
          service <- ZIO.service[ReviewService]
          reviews        <- service.getByCompanyId(1L)
          reviewNotFound <- service.getByCompanyId(999L)
        } yield assertTrue {
            reviews.toSet  == Set(goodReview, badReview)
            reviewNotFound.isEmpty
        }
      },

      ).provide(
        ReviewServiceLive.layer,
        stubRepoLayer
        )
  }


}

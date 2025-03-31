package com.rockthejvm.reviewboard.repositories

import java.time.Instant

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.*

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 1,
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
    review = "utter garbage",
    created = Instant.now(),
    updated = Instant.now()
    )


  override val initScript: String = "sql/reviews.sql"

  override def spec: Spec[Environment & Scope, Any] =
    suite("ReviewRepositorySpec")(

      test("create review") {
        val program = for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>

          review.management == goodReview.management &&
          review.culture == goodReview.culture &&
          review.salary == goodReview.salary &&
          review.benefits == goodReview.benefits &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review

        }

      },


      test("get by IDs (id, companyId, userId)") {
        for {
          repo               <- ZIO.service[ReviewRepository]
          review             <- repo.create(goodReview)
          fetchedById        <- repo.getById(review.id)
          fetchedByUserId    <- repo.getById(review.userId)
          fetchedByCompanyId <- repo.getById(review.companyId)
        } yield assertTrue {
          fetchedById.contains(review) &&
          fetchedByUserId.contains(review) &&
          fetchedByCompanyId.contains(review)
        }
      },


      test("get all") {
        for {
          repo               <- ZIO.service[ReviewRepository]
          good               <- repo.create(goodReview)
          bad                <- repo.create(badReview)
          all1               <- repo.getByUserId(1L)
          all2               <- repo.getByCompanyId(1L)
        } yield assertTrue(
            all1.toSet == Set(good, bad) &&
            all2.toSet == Set(good, bad)
          )
      },


      test("edit review") {
        for {
          repo      <- ZIO.service[ReviewRepository]
          review    <- repo.create(goodReview)
          updated   <- repo.update(review.id, r => r.copy(review="not too bad"))
        } yield assertTrue(
            review. id == updated. id &&
            review. companyId == updated. companyId &&
            review.userId == updated.userId &&
            review.management == updated.management &&
            review.culture == updated.culture &&
            review.salary == updated.salary &&
            review.benefits == updated.benefits &&
            review.wouldRecommend == updated.wouldRecommend &&
            updated.review == "not too bad" &&
            review.created == updated.created &&
            review.updated != updated.updated
          )
      },


      test("delete review") {
        for {
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(goodReview)
          _           <- repo.delete(review.id)
          mayBeReview <- repo.getById(review.id)
        } yield assertTrue(
                mayBeReview.isEmpty
              )
      },


    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.dbLayer,
      Scope.default
    )

}


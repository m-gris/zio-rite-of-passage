package com.rockthejvm.reviewboard.services

import zio.*

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.http.requests.ReviewCreationRequest
import org.flywaydb.core.internal.database.db2.DB2Type
import java.time.Instant
import com.rockthejvm.reviewboard.repositories.ReviewRepositoryLive

// for now... a 'thin' layer over the repository
trait ReviewService {
  def create(request: ReviewCreationRequest, userId: Long): Task[Review]   // the service 'delegates' to the repo to create a new row in DB
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]

}


class ReviewServiceLive private(/*
  a private constructor restricts the instantiation of a class to:
    - within the class itself
    - or its companion object.
  This is useful for controlling object creation,
  such as in singleton patterns or factory methods.*/
  repo: ReviewRepository) extends ReviewService {

    override def create(request: ReviewCreationRequest, userId: Long): Task[Review] =
      repo.create(Review(
        id = -1L, // will be create by the DB
        companyId=request.companyId,
        userId=userId,
        management=request.management,
        culture=request.culture,
        salary=request.salary,
        benefits=request.benefits,
        wouldRecommend=request.wouldRecommend,
        review=request.review,
        created=Instant.now(),
        updated=Instant.now()
        ))

    override def getById(id: Long): Task[Option[Review]] =
      repo.getById(id)

    override def getByCompanyId(companyId: Long): Task[List[Review]] =
      repo.getByCompanyId(companyId)

    override def getByUserId(userId: Long): Task[List[Review]] =
      repo.getByUserId(userId)

  }


object ReviewServiceLive {
  val layer = ZLayer {
    ZIO.service[ReviewRepository].map( repo => ReviewServiceLive(repo) )
  }
}

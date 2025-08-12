package com.rockthejvm.reviewboard.services

import zio.*

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.http.requests.ReviewCreationRequest
import org.flywaydb.core.internal.database.db2.DB2Type
import java.time.Instant

import com.rockthejvm.reviewboard.config.{Configs, SummaryConfig}
import com.rockthejvm.reviewboard.domain.data.ReviewSummary
import com.rockthejvm.reviewboard.repositories.Repository
import com.rockthejvm.reviewboard.repositories.ReviewRepositoryLive

// for now... a 'thin' layer over the repository
trait ReviewService {
  def create(request: ReviewCreationRequest, userId: Long): Task[Review]   // the service 'delegates' to the repo to create a new row in DB
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getSummary(companyId: Long): Task[Option[ReviewSummary]]
  def makeSummary(companyId: Long):  Task[Option[ReviewSummary]]
}


class ReviewServiceLive private(/*
  a private constructor restricts the instantiation of a class to:
    - within the class itself
    - or its companion object.
  This is useful for controlling object creation,
  such as in singleton patterns or factory methods.*/
  repo: ReviewRepository,
  summarize: SummarizationService,
  config: SummaryConfig,
  ) extends ReviewService {

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

    override def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
      repo.getSummary(companyId)

    override def makeSummary(companyId: Long): Task[Option[ReviewSummary]] =

      val randomReviews: Task[List[Review]] =
        getByCompanyId(companyId)
          .flatMap(reviews => Random.shuffle(reviews))
          .map(_.take(config.nSelected))

      val summary: Task[Option[String]] = randomReviews.flatMap { reviews =>
          if
            reviews.size < config.minReviews
          then
            ZIO.succeed(Some(s"Summarization requires at least ${config.minReviews}"))
          else
            buildPrompt(reviews).flatMap(summarize.llmCall)
        }

      summary.flatMap {
        case None => ZIO.none
        case Some(summary) => repo.insertSummary(companyId, summary).map(Some(_))
      }


    private def buildPrompt(reviews: List[Review]): Task[String] = ZIO.succeed {
      "You have the following reviews about a company:" + reviews.zipWithIndex.map {
        case (Review(_,_,_,management,culture,salary,benefits,wouldRecommend,review,_,_), index) =>
          s"""
            Review ${index + 1}:
              Management: $management stars / 5
              Culture: $culture stars / 5
              Salary: $salary stars / 5
              Benefits: $benefits stars / 5
              Net promoter score: $wouldRecommend stars / 5
              Content: "$review"
          """
      }
      .mkString("\n") +
      "Make a summary of all these reviews in at most one paragraph"
    }

  }


object ReviewServiceLive {

  val layer = ZLayer {
    for {
      repo      <- ZIO.service[ReviewRepository]
      summarize <- ZIO.service[SummarizationService]
      config    <- ZIO.service[SummaryConfig]
    } yield new ReviewServiceLive(repo, summarize, config)
  }

  val configuredLayer =
    Configs.makeLayer[SummaryConfig]("rockthejvm.summaries") >>> layer

}

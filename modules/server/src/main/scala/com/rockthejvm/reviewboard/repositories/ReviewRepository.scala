package com.rockthejvm.reviewboard.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.*
import java.time.Instant

trait ReviewRepository {
  // READING Ops
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]

  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]

  def getSummary(companyId: Long): Task[Option[ReviewSummary]]
  def insertSummary(companyId: Long, summary: String): Task[ReviewSummary]

}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {

  import quill.*

  // the "mapping rules"
  inline given reviewSchema: SchemaMeta[Review] =
    schemaMeta[Review]("reviews")
  // the "insertion rules"
  inline given reviewInsertSchema: InsertMeta[Review] =
    insertMeta[Review](/*excluding*/_.id, _.created, _.updated)
  // the "update rules"
  inline given reviewUpdateSchema: UpdateMeta[Review] =
    updateMeta[Review](/*excluding*/_.id, _.companyId, _.userId, _.created)


  def create(review: Review): Task[Review] =
    run(query[Review].insertValue(lift(review)).returning(r => r))

  def getById(id: Long): Task[Option[Review]] =
    run(query[Review].filter(_.id == lift(id))).map(_.headOption)

  def getByCompanyId(companyId: Long): Task[List[Review]] =
    run(query[Review].filter(_.companyId == lift(companyId)))

  def getByUserId(userId: Long): Task[List[Review]] =
    run(query[Review].filter(_.userId == lift(userId)))

  def update(id: Long, op: Review => Review): Task[Review] = for {
    current <- getById(id)
                    // would be Task[Option[Company]]
                    // but here we FORCE it to be company
                    .someOrFail(new RuntimeException(s"could not update missing ID $id"))
    updated <- run(
                query[Review]
                  .filter(_.id == lift(id))
                  .updateValue(lift(op(current)))
                  .returning(r => r)
                )
  } yield updated

  def delete(id: Long): Task[Review] =
    run(
      query[Review]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
      )

  // for the review_summaries Table
  inline given reviewSummarySchema: SchemaMeta[ReviewSummary] =
    schemaMeta[ReviewSummary]("review_summaries")
  inline given reviewSummaryInsertSchema: InsertMeta[ReviewSummary] =
    insertMeta[ReviewSummary]()
  inline given reviewSummaryUpdateSchema: UpdateMeta[ReviewSummary] =
    updateMeta[ReviewSummary]()

  def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
    run(
      query[ReviewSummary]
        .filter(_.companyId == lift(companyId))
      ) // List[ReviewSummary]
      .map(_.headOption) // Option[ReviewSummary]

  def insertSummary(companyId: Long, summary: String): Task[ReviewSummary] =

    val reviewSummary = ReviewSummary(companyId, summary, Instant.now())

    getSummary(companyId).flatMap {

      case None => run(
        query[ReviewSummary]
          .insertValue(lift(reviewSummary))
          .returning(r => r)

        )
      case Some(_) => run(
        query[ReviewSummary]
          .filter(_.companyId == lift(companyId))
          .updateValue(lift(reviewSummary))
          .returning(r => r))
    }

}

object ReviewRepositoryLive {
  val layer = ZLayer { for {
    quill <- ZIO.service[Quill.Postgres[SnakeCase]]
  } yield ReviewRepositoryLive(quill)
  }
}

object ReviewRepositoryPlayground extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    val program = for {
      repo    <- ZIO.service[ReviewRepository]
      _       <- repo.insertSummary(1, "This is a summary of reviews")
      summary <- repo.getSummary(1)
      _       <- Console.printLine(summary)
      _       <- repo.insertSummary(1, "NEW summary")
      summary2 <- repo.getSummary(1)
      _       <- Console.printLine(summary2)
    } yield ()

    program.provide(
      ReviewRepositoryLive.layer,
      Repository.dataLayer
    )

}

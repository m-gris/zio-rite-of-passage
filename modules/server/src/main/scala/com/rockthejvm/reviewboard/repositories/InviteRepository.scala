package com.rockthejvm.reviewboard.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.*

trait InviteRepository {
  def getByUserName(userName: String): Task[List[Invitations]]
  def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]]
  def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long]
  def activatePack(id: Long): Task[Boolean]
  def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int]
}

class InviteRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends InviteRepository {

  import quill.*

  // NB: INLINE GIVEN
  // => allows QUILL MACROS to generate TYPE SAFE QUERIES
  // @@@@@@@@@@ LIVE AT COMPILE TIME @@@@@@@@@@
  // will also respect the column / field ORDERS and prevent accidently 'swapping' values of the same type ...
  inline given schema: SchemaMeta[InviteRecord] =
    schemaMeta[InviteRecord]("invites" /* the table name*/ )
  inline given upMeta: UpdateMeta[InviteRecord] =
    updateMeta[InviteRecord](_.id /*col omitted from the update statement */ )
  inline given insMeta: InsertMeta[InviteRecord] =
    insertMeta[InviteRecord](_.id /*col omitted from the insert statement*/ )

  inline given companySchema: SchemaMeta[Company] =
    schemaMeta[Company]("companies" /* the table name*/ )
  inline given companUpMeta: UpdateMeta[Company] =
    updateMeta[Company](_.id /*col omitted from the update statement */ )
  inline given companInsMeta: InsertMeta[Company] =
    insertMeta[Company](_.id /*col omitted from the insert statement*/ )

  override def getByUserName(userName: String): Task[List[Invitations]] =
    run(
      for { // a JOIN STATEMENT in quill can be expressed as a for-comprehension
        record <- query[InviteRecord]
          .filter(_.userName == lift(userName))
          .filter(_.nInvites > 0)
          .filter(_.active)
        company <- query[Company]
        /* the JOIN CONDITION as a `if` guard */
        if company.id == record.companyId

      } yield Invitations(company.id, company.name, record.nInvites)
    )

  override def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]] =
    run(
      query[InviteRecord]                       // select * from invites
        .filter(_.companyId == lift(companyId)) // where ...
        .filter(_.userName == lift(userName))   // and ...
        .filter(_.active)                       // and ...
    ).map(_.headOption)                         // Option[InviteRecord]

  // WARNING: adding multiple packs for the same company may cause unexpected behavior
  override def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long] =
    run(
      query[InviteRecord]
        .insertValue(lift(InviteRecord(-1, userName, companyId, nInvites, false)))
        .returning(createdPack => createdPack.id)
    )

  override def activatePack(id: Long): Task[Boolean] =
    for {
      current <- run(query[InviteRecord].filter(_.id == lift(id)))
        .map(_.headOption)
        .someOrFail(new RuntimeException(s"Unable to activate packId $id"))
      result <- run(
        query[InviteRecord]
          .filter(_.id == lift(id))
          .updateValue(lift(current.copy(active = true)))
          .returning(_ => true)
      )
    } yield result

  override def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int] =
    for {
      currentRecord <- getInvitePack(userName, companyId)
        .someOrFail(
          new RuntimeException(
            s"User $userName cannot send invite for company $companyId"
          )
        )
      nInvitesMarked <- ZIO.succeed(Math.min(nInvites, currentRecord.nInvites))
      _ <- run(
        query[InviteRecord]
          .filter(_.id == lift(currentRecord.id))
          .updateValue(lift(currentRecord.copy(nInvites = currentRecord.nInvites - nInvitesMarked)))
          .returning(r => r)
      )

    } yield nInvitesMarked

}

object InviteRepositoryLive {

  val layer = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield InviteRepositoryLive(quill)
  }

}

// TODO: write a test instead of this 'manual one'
object InviteRepositoryDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    val program = for {

      repo    <- ZIO.service[InviteRepository]
      records <- repo.getByUserName("daniel@rockthejvm.com")
      _       <- Console.printLine(s"Records: ${records}")
    } yield ()
    program.provide(
      InviteRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}

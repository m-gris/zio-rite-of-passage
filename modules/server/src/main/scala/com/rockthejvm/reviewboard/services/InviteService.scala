package com.rockthejvm.reviewboard.services

import zio.*

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.*

trait InviteService {

  def getByUserName(userName: String): Task[List[Invitations]]

  def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[Int /* n invites */ ]

  def addInvitePack(userName: String, companyId: Long): Task[Long /* the packId */ ]

}

class InviteServiceLive private (
    inviteRepository: InviteRepository,
    companyRepository: CompanyRepository
) extends InviteService {

  override def getByUserName(userName: String): Task[List[Invitations]] =
    inviteRepository.getByUserName(userName)

  override def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[Int /* n invites */ ] =
    ZIO.fail(new RuntimeException("Not implemented yet..."))

  override def addInvitePack(userName: String, companyId: Long): Task[Long /* the packId */ ] =
    for {
      company <- companyRepository
        .getById(companyId)
        .someOrFail( // forcing it to exists or not...
          new RuntimeException(s"Cannot invite to review. Company $companyId does not exist")
        )
      currentPack <- inviteRepository.getInvitePack(userName, companyId)
      newPackId <- currentPack match {
        case None /* happy path */ =>
          inviteRepository.addInvitePack(userName, companyId, 200) // TODO configure this
        case Some(_) =>
          ZIO.fail(new RuntimeException("You already have an active pack for this company"))
      }
      // TODO remove when introducing the payment process
      _ <- inviteRepository.activatePack(newPackId)
    } yield newPackId

}

object InviteServiceLive {
  val layer = ZLayer {
    for {
      inviteRepo  <- ZIO.service[InviteRepository]
      companyRepo <- ZIO.service[CompanyRepository]
    } yield new InviteServiceLive(inviteRepo, companyRepo)
  }
}

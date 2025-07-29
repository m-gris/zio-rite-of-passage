package com.rockthejvm.reviewboard.services

import zio.*

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.config.InvitePackConfig
import com.rockthejvm.reviewboard.config.Configs

trait InviteService {

  def getByUserName(userName: String): Task[List[Invitations]]

  def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[Int /* n invites */ ]

  def addInvitePack(userName: String, companyId: Long): Task[Long /* the packId */ ]

  def activatePack(id: Long): Task[Boolean]

}

class InviteServiceLive private (
    inviteRepository: InviteRepository,
    companyRepository: CompanyRepository,
    emailService: EmailService,
    config: InvitePackConfig
) extends InviteService {

  override def getByUserName(userName: String): Task[List[Invitations]] =
    inviteRepository.getByUserName(userName)

  override def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[Int /* n invites */ ] =
    for {
      company <- companyRepository
        .getById(companyId)
        .someOrFail( // forcing it to exists or not...
          new RuntimeException(s"Cannot send invites. Company $companyId does not exist")
        )
      nInvitesMarked <- inviteRepository.markInvites(userName, companyId, receivers.size)
      _ <- ZIO.collectAllPar(
        receivers
          .take(nInvitesMarked)
          map (receiver => emailService.sendInvite(userName, receiver, company))
      )
    } yield nInvitesMarked

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
          inviteRepository.addInvitePack(userName, companyId, config.nInvites)
        case Some(_) =>
          ZIO.fail(new RuntimeException("You already have an active pack for this company"))
      }
      // TODO remove when introducing the payment process
      // _ <- inviteRepository.activatePack(newPackId)
    } yield newPackId

  override def activatePack(id: Long): Task[Boolean] = inviteRepository.activatePack(id)

}

object InviteServiceLive {
  val layer = ZLayer {
    for {
      inviteRepo   <- ZIO.service[InviteRepository]
      companyRepo  <- ZIO.service[CompanyRepository]
      emailService <- ZIO.service[EmailService]
      config       <- ZIO.service[InvitePackConfig]
    } yield new InviteServiceLive(inviteRepo, companyRepo, emailService, config)
  }

  val configuredLayer = Configs.makeLayer[InvitePackConfig]("rockthejvm.invites") >>> layer
}

package com.rockthejvm.reviewboard.components

import zio.*
import org.scalajs.dom
import com. raquo. laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.common.Constants.emailRegex
import com.rockthejvm.reviewboard.http.requests.InviteRequest

object InviteActions {

  val invitationsBus = EventBus[List[Invitations]]()
  private val errorStore = Var[Option[String]](None)

  def refreshInviteList() =
    useBackend(_.inviteEndpoints.getByUserNameEndpoint(()) )

  def apply() =
    div(
      onMountCallback(_ => refreshInviteList().emitTo(invitationsBus)),
      cls := "profile-section",
      h3(span("Invites")),
      children <-- invitationsBus.events.map(_.sortBy(_.companyName).map(renderInviteSection))
    )

  def renderInviteSection(invitations: Invitations) = {

    val emails = Var[Array[String]](Array())

    def isInvalid(email: String) = !email.matches(emailRegex)

    val inviteSubmiter = Observer[Unit] { _ =>
      val currentEmails = emails.now().toList
      if currentEmails.exists(isInvalid)
        then errorStore.set(Some("At least an email is invalid"))
      else
        val refreshProgram = for {
          response <- useBackend(_.inviteEndpoints.inviteEndpoint(InviteRequest(invitations.companyId, currentEmails)))
          _ <- ZIO.succeed {
            // Success - clear error and textarea
            errorStore.set(None)
            emails.set(Array())
          }
          invitesLeft <- refreshInviteList()
        } yield invitesLeft
        
        refreshProgram
          .catchAll { error =>
            errorStore.set(Some(s"Failed to send invites: ${error.getMessage}"))
            // Still need to refresh the list to show updated invite count
            refreshInviteList()
          }
          .emitTo(invitationsBus)
    }


    def renderError(maybeError: Option[String]) = maybeError.map { message =>
        div( cls := "page-status-errors", message)
    }

    div(
      cls := "invite-section",
      h5(span(invitations.companyName)),
      p(s"${invitations.nInvites} invites left"),
      textArea(
        cls := "invites-text-area",
        placeholder := "Enter emails, one per line",
        onInput.mapToValue.map(
          _.split("\n")
          .map(_.trim)
          .filter(_.nonEmpty)) --> emails.writer
          ),
      button(
        `type`:= "button",
        cls := "btn btn-primary",
        "Invite",
        onClick.mapToUnit --> inviteSubmiter
        ),
      child.maybe <-- errorStore.signal.map(renderError)
      )


  }


}

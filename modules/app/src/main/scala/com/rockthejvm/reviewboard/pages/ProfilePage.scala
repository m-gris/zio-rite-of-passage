package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom
import com. raquo. laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.components.Anchors

object ProfilePage {

  def apply() =

    div(

      cls := "row",

      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            src := Constants.logo,
            alt := "Rock the JVM" // alt => "alternative text", i.e if the image can't be loaded
          )
        )
      ),

      div(
        cls := "col-md-7",
        div(
          cls := "form-section",
          child <-- Session.userState.signal.map {
            case None => renderInvalid()
            case Some(_) => renderContent()
          }
        )
      )
    )

  private def renderInvalid() =
    div(
      cls := "top-section",
      h1(span("Oops!")),
      div("It seems you're not logged in.")
      )

  private def renderContent() =
    div(
      cls := "top-section",
      h1(span("Profile")),
      // change password section
      div(
        cls := "profile-section",
        h3(span("Account Settings")),
        Anchors.renderNavLink("Change Password", "/changepassword")
        ),
      // actions section: send invites for every company they have invites for
      InviteActions()

    )

}

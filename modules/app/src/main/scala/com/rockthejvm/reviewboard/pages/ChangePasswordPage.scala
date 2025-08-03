package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom
import com. raquo. laminar.api.L.{*, given}
import com.raquo.airstream.state.Var
import org.scalajs.dom.html.Element
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.http.requests.PasswordUpdateRequest
import com.raquo.airstream.core.Observer

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.ZJS.useBackend
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.core.Session


case class PasswordChangeState(
  oldPassword: String = "",
  newPassword: String = "",
  newPasswordConfirmation: String = "",
  upStreamStatus: Option[Either[String, String]] = None,
  override val showStatus: Boolean = false,
  ) extends FormState {

    override val potentialErrors: List[Option[String]] =  List(
      Option.when(oldPassword.isEmpty)("Old password is empty"),
      Option.when(newPassword.isEmpty)("New password is empty"),
      Option.when(newPasswordConfirmation != newPassword)("New password mistmatch")
      ) ++ upStreamStatus.map(_.left.toOption).toList

    override def maybeSuccess: Option[String] = upStreamStatus.flatMap(_.toOption)
  }

object ChangePasswordPage extends FormPage[PasswordChangeState](title="Profile"){

  override val blankSlate = PasswordChangeState()

  override val state: Var[PasswordChangeState] = Var(blankSlate)

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = {

    Session.getUserState().map( userSession => List(

            renderInput(
              name="Password",
              input_type="password-input",
              uid = "password",
              isRequired = true,
              placeHolder = "Your password",
              updateFn = (s, p) => s.copy(oldPassword=p, showStatus = false, upStreamStatus = None)
            ),

            renderInput(
              name="New Password",
              input_type="new-password-input",
              uid = "password",
              isRequired = true,
              placeHolder = "New password",
              updateFn = (s, p) => s.copy(newPassword=p, showStatus = false, upStreamStatus = None)
            ),

            renderInput(
              name="Confirm New Password",
              input_type="confirm-new-password-input",
              uid = "password",
              isRequired = true,
              placeHolder = "Confirm New password",
              updateFn = (s, p) => s.copy(newPasswordConfirmation=p, showStatus = false, upStreamStatus = None)
            ),

            button(
              `type` := "button",
              "Change password",
              onClick
                .preventDefault // i.e override the default behavior
                .mapTo(state.now()) /* submitting/emitting the current state */ --> submiter(userSession.email) /* Observer[State]*/
            )

    )
  ).getOrElse(
    List(
      div(
        cls := "centered-text",
        "Oups... You are not logged-in yet."
        )
      )
    )



  }

  def submiter(email: String) = Observer[PasswordChangeState] { thisState =>
    if thisState.hasErrors then {
      state.update(_.copy(showStatus=true))
    } else {
      useBackend(_.userEndpoints.passwordUpdateEndpoint(
        PasswordUpdateRequest(email=email, oldPassword=thisState.oldPassword, newPassword = thisState.newPassword)
        )
      )

        // IF SUCCESSFUL BACKEND-CALL
        .map { (response: UserResponse) =>
          state.update(_.copy(showStatus = true, upStreamStatus = Some(Right("Password successfuly changed"))))
        }
        // IF FAILURE IN BACKEND-CALL
        .tapError { (error: Throwable) =>
          ZIO.succeed {
            state.update(_.copy(showStatus = true, upStreamStatus = Some(Left(error.getMessage))))
          }
        }
        // ZIO EFFECT DESCRIBED .. Now must execut it !
        .runJs
    }
  }

}

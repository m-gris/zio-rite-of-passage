package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom.html.Element
import org.scalajs.dom.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.airstream.state.Var
import com.rockthejvm.reviewboard.common.Constants.emailRegex
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.requests.ResetPasswordRequest
import com.rockthejvm.reviewboard.components.Anchors

case class RecoverPasswordState(
  email: String = "",
  token: String = "",
  newPassword: String = "",
  confirmationPassword: String = "",
  override val showStatus: Boolean = false,
  val upstreamStatus: Option[Either[String, String]] = None
  ) extends FormState {

    def serverError = upstreamStatus.map(_.left.toOption).toList

    override def potentialErrors: List[Option[String]] = List(

      // Option.unless(DESIRED STATE)(ERROR-MESSAGE)
      Option.unless(email.matches(emailRegex))(s"$email is not a valid email"),
      Option.unless(token.nonEmpty)("token cannot be empty"),

      // Option.when(ERROR)(ERROR-MESSAGE)
      Option.when(newPassword.isEmpty)("password cannot be empty"),
      Option.when(newPassword != confirmationPassword)("passwords do not match"),

      ) ++ serverError

    override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

  }

object RecoverPasswordPage extends FormPage[RecoverPasswordState](title="Recover Password"){

  override val blankSlate = RecoverPasswordState()
  override val state: Var[RecoverPasswordState] = Var(blankSlate)

  val submiter = Observer[RecoverPasswordState] { thisState =>
      if thisState.hasErrors then {
        state.update(_.copy(showStatus=true))
      } else {
        useBackend(_.userEndpoints.resetPasswordEndpoint(ResetPasswordRequest(thisState.email, thisState.token, thisState.newPassword)))
          // IF SUCCESSFUL BACKEND-CALL
          .map { _ => state.update(_.copy(showStatus = true, upstreamStatus = Some(Right("Success! You can login now.")))) }
          // IF FAILURE IN BACKEND-CALL
          .tapError { (error: Throwable) =>
            ZIO.succeed {
              state.update(_.copy(showStatus = true, upstreamStatus = Some(Left(error.getMessage))))
            }
          }
          // ZIO EFFECT DESCRIBED .. Now must execut it !
          .runJs
      }
    }


  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(

          renderInput(
            name="Email", input_type="text", uid = "email-input", isRequired = true,
            placeHolder = "Your Email", updateFn = (s, e)=>s.copy(email=e, showStatus = false, upstreamStatus = None)
          ),

          renderInput(
            name="Recovery Token (from email)", input_type="text", uid = "token-input", isRequired = true,
            placeHolder = "The token", updateFn = (s, t)=>s.copy(token=t, showStatus = false, upstreamStatus = None)
          ),

          renderInput(
            name="New Password", input_type="text", uid = "password-input",
            isRequired = true, placeHolder = "Your New Password", updateFn = (s, p) => s.copy(newPassword=p, showStatus = false, upstreamStatus = None)
          ),

          renderInput(
            name="Confirm Password", input_type="text", uid = "confirmation-password-input",
            isRequired = true, placeHolder = "Confirm your Password",
            updateFn = (s, p) => s.copy(confirmationPassword=p, showStatus = false, upstreamStatus = None)
          ),

          button(
            `type` := "button",
            "Reset Password",
            onClick
              .preventDefault // i.e override the default behavior
              .mapTo(state.now()) /* submitting/emitting the current state */ --> submiter /* Observer[State]*/
          ),

        Anchors.renderNavLink(
          "Need a password recovery token?",
          "/forgotpassword",
          "auth-link" // css
        )

    )

}

package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom.html.Element
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.common.Constants.emailRegex
import com.rockthejvm.reviewboard.http.requests.ForgotPasswordRequest
import com.rockthejvm.reviewboard.components.Anchors


case class ForgotPasswordState(
  email: String = "",
  showStatus: Boolean = false,
  upstreamStatus: Option[Either[String, String]] = None,
  ) extends FormState {

  override def potentialErrors: List[Option[String]] = List(

    Option.unless(email.matches(emailRegex))(s"$email is not a valid email")

      ) ++ upstreamStatus.map(_.left.toOption).toList

  override def maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

}

object ForgotPasswordPage extends FormPage[ForgotPasswordState](title = "Forgot Password") {

  override val blankSlate = ForgotPasswordState()
  override val state: Var[ForgotPasswordState] = Var(blankSlate)

  val submiter = Observer[ForgotPasswordState] { thisState =>
    if thisState.hasErrors then {
      state.update(_.copy(showStatus=true))
    } else {
      useBackend(_.userEndpoints.forgotPasswordEndpoint(ForgotPasswordRequest(thisState.email)))
        // IF SUCCESSFUL BACKEND-CALL
        .map { _ =>
          state.update(_.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Check your email")))
          )
        }
        // IF FAILURE IN BACKEND-CALL
        .tapError { (error: Throwable) =>
          ZIO.succeed {
            state.update(_.copy(showStatus = true, upstreamStatus  = Some(Left(error.getMessage))))
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

    button(
      `type` := "button",
      "Recover Password",
      onClick
        .preventDefault // i.e override the default behavior
        .mapTo(state.now()) --> submiter /* Observer[State]*/
      ),

    Anchors.renderNavLink(
      "Have a password recovery token?",
      "/recoverpassword",
      "auth-link" // css
    )

 )

}

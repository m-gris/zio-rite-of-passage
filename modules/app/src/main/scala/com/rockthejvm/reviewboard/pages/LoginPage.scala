package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.domain.data.UserSession
import com.rockthejvm.reviewboard.common.Constants.emailRegex
import com.rockthejvm.reviewboard.http.requests.UserLoginRequest
import frontroute.BrowserNavigation
import com.rockthejvm.reviewboard.components.Anchors.renderNavLink


case class LoginFormState(
  email: String = "",
  password: String = "",
  override val showStatus: Boolean = false,
  upStreamError: Option[String] = None
) extends FormState {

  private val emailError = Option.when(!email.matches(emailRegex))("Email is invalid")
  private val passwordError = Option.when(password.isEmpty)("Password is empty")
  override val potentialErrors = List(emailError, passwordError, upStreamError)
  // Pure validation - no UI concerns
  val validationErrors = potentialErrors.find(_.isDefined).flatten
  // UI presentation logic
  val mustBeShown = (error: String) => showStatus
  val displayableErrors = validationErrors.filter(mustBeShown)

  override val maybeSuccess: Option[String] = None

}

object LoginPage extends FormPage[LoginFormState](title="Log In"){


  override val blankSlate = LoginFormState()
  // REACTIVE VARIABLE
  // can be updated whenever a user types into the 'inputs'
  override val state = Var(blankSlate)

  // SUBMIT THE FORM
  val submiter = Observer[LoginFormState] { thisState =>
    if thisState.hasErrors then {
      state.update(_.copy(showStatus=true))
    } else {
      useBackend(_.userEndpoints.userLoginEndpoint(UserLoginRequest(thisState.email, thisState.password)))
        // IF SUCCESSFUL BACKEND-CALL
        .map { (session: UserSession) =>
          Session.setUserState(session)
          state.set(LoginFormState()) // flush / refresh the state
          BrowserNavigation.replaceState("/") // go to "home" after succesful login
        }
        // IF FAILURE IN BACKEND-CALL
        .tapError { (error: Throwable) =>
          ZIO.succeed {
            state.update(_.copy(showStatus = true, upStreamError = Some(error.getMessage)))
          }
        }
        // ZIO EFFECT DESCRIBED .. Now must execut it !
        .runJs
    }
  }

  // NOTE: CONTENT OF THE FORM
  def renderChildren() = List(

            renderInput(
              name="Email", input_type="text", uid = "email-input", isRequired = true,
              placeHolder = "Your Email", updateFn = (s, e)=>s.copy(email=e, showStatus = false, upStreamError = None)
            ),

            renderInput(
              name="Password", input_type="text", uid = "password-input",
              isRequired = true, placeHolder = "You Password", updateFn = (s, p) => s.copy(password=p, showStatus = false, upStreamError = None)
            ),

            button(
              `type` := "button",
              "Log In",
              onClick
                .preventDefault // i.e override the default behavior
                .mapTo(state.now()) /* submitting/emitting the current state */ --> submiter /* Observer[State]*/
            ),

          renderNavLink(
            "Forgot password?", // the message
            "/forgotpassword", // the url
            "auth-link" // the css style
            )


    )


}

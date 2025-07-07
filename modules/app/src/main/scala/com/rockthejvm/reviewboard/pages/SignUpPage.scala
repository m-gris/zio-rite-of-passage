package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom.html
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.common.Constants.emailRegex
import com.rockthejvm.reviewboard.http.requests.UserRegistration
import com.rockthejvm.reviewboard.http.responses.UserResponse



case class SignUpState(
  email: String = "",
  password: String = "",
  confirmationPassword: String = "",
  override val showStatus: Boolean = false,
  upStreamStatus: Option[Either[String, String]] = None
  ) extends FormState {

  private val emailError = Option.when(!email.matches(emailRegex))("Email is invalid")
  private val passwordError = Option.when(password.isEmpty)("Password is empty")
  private val confirmationPasswordError = Option.when(confirmationPassword != password)("The 2 Passwords Mismatch")

  override def potentialErrors: List[Option[String]] =
    List(emailError, passwordError, confirmationPasswordError)
    ++ upStreamStatus.map(status => status.left.toOption).toList

  override def maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption) // Either being "Right biases", we can invoke .toOption directly (i.e without _.right.toOption)

  }

object SignUpPage extends FormPage[SignUpState](title="Sign Up") {

  override val blankSlate = SignUpState()
  override val state = Var(blankSlate)

  val submiter = Observer[SignUpState] { thisState =>
    if thisState.hasErrors then {
      state.update(_.copy(showStatus=true))
    } else {
      useBackend(_.userEndpoints.userRegistrationEndpoint(UserRegistration(email=thisState.email, password=thisState.password)))
        // IF SUCCESSFUL BACKEND-CALL
        .map { (response: UserResponse) =>
          state.update(_.copy(showStatus = true, upStreamStatus = Some(Right("Account Created. You can log in now."))))
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

  override def renderChildren(): List[ReactiveHtmlElement[html.Element]] = List(

            renderInput(
              name="Email", input_type="text", uid = "email-input", isRequired = true,
              placeHolder = "Your Email", updateFn = (s, e)=>s.copy(email=e, showStatus = false, upStreamStatus = None)
            ),

            renderInput(
              name="Password", input_type="text", uid = "password-input",
              isRequired = true, placeHolder = "Your Password", updateFn = (s, p) => s.copy(password=p, showStatus = false, upStreamStatus = None)
            ),


            renderInput(
              name="Confirm Password", input_type="text", uid = "confirmation-password-input",
              isRequired = true, placeHolder = "Confirm your Password",
              updateFn = (s, p) => s.copy(confirmationPassword=p, showStatus = false, upStreamStatus = None)
            ),

            button(
              `type` := "button",
              "Sign Up",
              onClick
                .preventDefault // i.e override the default behavior
                .mapTo(state.now()) /* submitting/emitting the current state */ --> submiter /* Observer[State]*/
            )

    )

}

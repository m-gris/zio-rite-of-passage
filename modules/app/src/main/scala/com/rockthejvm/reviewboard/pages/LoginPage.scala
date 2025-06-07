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

object LoginPage {

  case class State(
    email: String = "",
    password: String = "",
    showStatus: Boolean = false,
    upStreamError: Option[String] = None
  ) {
    val emailError = Option.when(!email.matches(emailRegex))("Email is invalid")
    val passwordError = Option.when(password.isEmpty)("Password is empty")
    val potentialErrors = List(emailError, passwordError, upStreamError)
    // Pure validation - no UI concerns
    val validationErrors = potentialErrors.find(_.isDefined).flatten
    // UI presentation logic
    val mustBeShown = (error: String) => showStatus
    val displayableErrors = validationErrors.filter(mustBeShown)
    val hasErrors: Boolean = validationErrors.isDefined

  }

  // REACTIVE VARIABLE
  // can be updated whenever a user types into the 'inputs'
  val state = Var(State())

  val submiter = Observer[State] { thisState =>
    if thisState.hasErrors then {
      state.update(_.copy(showStatus=true))
    } else {
      useBackend(_.userEndpoints.userLoginEndpoint(UserLoginRequest(thisState.email, thisState.password)))
        // IF SUCCESSFUL BACKEND-CALL
        .map { (session: UserSession) =>
          Session.setUserState(session)
          state.set(State()) // flush / refresh the state
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
        // right
        div(
          cls := "form-section",
          div(cls := "top-section", h1(span("Log In"))),
          children <-- state.signal
                              .map( (state: State) => state.displayableErrors.map(renderError) )
                              .map(_.toList),
          renderSuccess(),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
            renderChildren()
          )
        )
      )
    )

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
            )

    )
  def renderError(error: String) =
      div(
        cls := "page-status-errors",
        error
      )

  def renderSuccess(shouldShow: Boolean = false) =
    if !shouldShow then div() else
      div(
        cls := "page-status-success",
        // "This is a success"
        // HACKISH... Just to check / display
        child.text <-- state.signal.map(_.toString)
      )

  def renderInput(
    name: String,
    input_type: String,
    uid: String,
    isRequired: Boolean,
    placeHolder: String,
    updateFn: (State, String) => State
  ) =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := "form-id-1-todo",
            cls   := "form-label",
            if isRequired then span("*") else span(),
            name,
          ),
        input(
          `type`      := "text",
          cls         := "form-control",
          idAttr      := uid,
          placeholder := placeHolder,

          // mapToValue needed to get a Stream of Strings...
          // otherwise would surfaces untyped HTML inputs
          onInput.mapToValue --> /* fed into an Observer[String] */ state.updater(updateFn)
        )
      )
    )
  )

}

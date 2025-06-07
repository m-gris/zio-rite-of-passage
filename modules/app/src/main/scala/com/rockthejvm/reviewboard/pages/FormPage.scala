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
import com.raquo.laminar.nodes.ReactiveHtmlElement


trait FormState {

  def showStatus: Boolean

  def potentialErrors: List[Option[String]]
  def maybeError: Option[String] = potentialErrors.find(_.isDefined).flatten
  def hasErrors: Boolean = maybeError.isDefined

  def maybeSuccess: Option[String]

  def maybeStatus: Option[Either[String, String]] = {
    maybeError.map(Left(_))
      .orElse(maybeSuccess.map(Right(_)))
      .filter(_ => showStatus)
  }

}


abstract class  FormPage[S <: FormState](title: String){

  // REACTIVE VARIABLE
  // can be updated whenever a user types into the 'inputs'
  val state: Var[S]

  def renderChildren(): List[ReactiveHtmlElement[dom.html.Element]]

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
          div(cls := "top-section", h1(span(title))),
          children <-- state.signal
            .map(_.maybeStatus)
            .map(renderStatus)
            .map(_.toList),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
            renderChildren()
          )
        )
      )
    )


  def renderStatus(status: Option[Either[String,String]]) = status.map {
    case Left(error) =>
      div(
        cls := "page-status-errors",
        error
      )
    case Right(msg) =>
      div(
        cls := "page-status-success",
        child.text <-- state.signal.map(_.toString)
      )
  }


  def renderInput(
    name: String,
    input_type: String,
    uid: String,
    isRequired: Boolean,
    placeHolder: String,
    updateFn: (S, String) => S
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

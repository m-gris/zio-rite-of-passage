package com.rockthejvm.reviewboard.components

import frontroute.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

object Router {

  def apply() =

    mainTag(
      routes(
        div(
          cls := "container-fluid",
          pathEnd { // localhost:1234 or  localhost:1234/
            div("main page")
          },
          path("companies") { // localhost:1234/companies
            div("companies page")
          }
          )
        )
      )
}

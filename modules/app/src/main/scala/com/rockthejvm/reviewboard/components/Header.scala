package com.rockthejvm.reviewboard.components

import frontroute.*
import org.scalajs.dom
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}

object Header {
  def apply() =
    div(
      cls := "container-fluid p-0",
      div(
        cls := "jvm-nav",
        div(
          cls := "container",
          navTag(
            cls := "navbar navbar-expand-lg navbar-light JVM-nav",
            div(
              cls := "container",
              // TODO logo
              button(
                cls                                         := "navbar-toggler",
                `type`                                      := "button",
                htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
                htmlAttr("data-bs-target", StringAsIsCodec) := "#navbarNav",
                htmlAttr("aria-controls", StringAsIsCodec)  := "navbarNav",
                htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
                htmlAttr("aria-label", StringAsIsCodec)     := "Toggle navigation",
                span(cls := "navbar-toggler-icon")
              ),
            div(
              cls    := "collapse navbar-collapse",
              idAttr := "navbarNav",
              ul(
                cls := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3"
                // TODO children
              )
            )
          )
        )
      )
    )
  )
}

package com.rockthejvm.reviewboard.components

import frontroute.*
import org.scalajs.dom
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.annotation.*
import scala.scalajs.js // object referencing the entire JavaScript Api

import com.rockthejvm.reviewboard.common.*
object Header {
  def apply() = // boiler-platty stuff
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
              renderLogo(),
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
              ul( // un-ordered list
                cls := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3",
                render(navLinks)
              )
            )
          )
        )
      )
    )
  )

  @js.native // bridges scala native strings to js native strings
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/", // i.e clicking on the logo redirects to the home page
      cls := "navbar-brand",
      img(
        cls := "home-logo",
        src := logoImage,
        alt := "Rock the JVM" // alt => "alternative text", i.e if the image can't be loaded
        )
  )

  val navLinks = List(
    NavLink("Companies", "/companies"),
    NavLink("Log In", "/login"),
    NavLink("Sign Up", "/signup"),
    )

  private def render(navLink: NavLink) = li(
    cls := "nav-item",
    Anchors.renderNavLink(navLink.label, navLink.location, "nav-link jvm-item")
    )

  private def render(navLinks: List[NavLink]): List[HtmlElement] = navLinks.map(render)


}

case class NavLink(label: String, location: String)


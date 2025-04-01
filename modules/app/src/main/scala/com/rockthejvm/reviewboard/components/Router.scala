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

          /*
             DIRECTIVES
             ----------
             In the context of Scala web routing (specifically with the `frontroute` library ),
             directives are BUILDING BLOCKS THAT COMPOSE THE ROUTING LOGIC.
             They are similar to pattern matchers
             and help determine what code should be executed based on the URL path and other HTTP request properties.
           * */

          (pathEnd | path("companies"))  {
            // localhost:1234 or  localhost:1234/ or localhost:1234/companies or localhost:1234/companies/
            // all redirect to the main page
            div("main page")
          },

          path("login") {
            // localhost:1234/login
            div("login page")
          },

          path("signup") {
            // localhost:1234/signup
            div("signup page")
          },

          noneMatched {
            div("404 page not found - Are you lost ?")
          }

          )
        )
      )
}

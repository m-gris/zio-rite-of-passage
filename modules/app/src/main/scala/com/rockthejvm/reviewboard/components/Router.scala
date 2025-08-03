package com.rockthejvm.reviewboard.components

import frontroute.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.pages.*

object Router {

  val externalUrlBus = EventBus[String]() // to redirect the entire UI to external pages (ex: Stripe for checkout)

  def redirect(url: String): Unit = { dom.window.location.href = url }

  def apply() =

    mainTag(

      onMountCallback(ctx =>
          externalUrlBus.events.foreach(redirect)(ctx.owner)
          ),

      routes(
        div(

          cls := "container-fluid",

          /*
             DIRECTIVES
             ----------
             In the context of Scala web routing (specifically with the `frontroute` library ),
             `directives` are BUILDING BLOCKS THAT COMPOSE THE ROUTING LOGIC.
             They are similar to pattern matchers and help determine what code
             should be executed based on the URL path and other HTTP request properties.
           * */

          (pathEnd | path("companies"))  {
            // localhost:1234 or  localhost:1234/ or localhost:1234/companies or localhost:1234/companies/
            // all redirect to the main page
            CompaniesPage()
          },

          path("changepassword") {
            ChangePasswordPage()
          },

          path("profile") {
            ProfilePage()
          },

          path("login") {
            LoginPage()
          },

          path("signup") {
            SignUpPage()
          },

          path("logout") {
            LogoutPage()
          },

          path("post") {
            CreateCompanyPage()
          },

          path("company" / long) { companyId =>
            CompanyPage(companyId)
          },

          path("forgotpassword") {
            ForgotPasswordPage()
          },

          path("recoverpassword") {
            RecoverPasswordPage()
          },

          noneMatched {
            NotFoundPage()
          }

          )
        )
      )
}

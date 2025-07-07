package com.rockthejvm.reviewboard.components

import frontroute.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.pages.*

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
             `directives` are BUILDING BLOCKS THAT COMPOSE THE ROUTING LOGIC.
             They are similar to pattern matchers and help determine what code
             should be executed based on the URL path and other HTTP request properties.
           * */

          (pathEnd | path("companies"))  {
            // localhost:1234 or  localhost:1234/ or localhost:1234/companies or localhost:1234/companies/
            // all redirect to the main page
            CompaniesPage()
          },

          path("profile") {
            // localhost:1234/profile
            ProfilePage()
          },

          path("login") {
            // localhost:1234/login
            LoginPage()
          },

          path("signup") {
            // localhost:1234/signup
            SignUpPage()
          },

          path("logout") {
            // localhost:1234/logout
            LogoutPage()
          },

          path("post") {
            // localhost:1234/post
            CreateCompanyPage()
          },

          path("company" / long) { companyId =>
            // localhost:1234/company/<id>
            CompanyPage(companyId)
          },

          path("forgotpassword") {
            // localhost:1234/forgotpassword
            ForgotPasswordPage()
          },

          path("recoverpassword") {
            // localhost:1234/recoverpassword
            RecoverPasswordPage()
          },

          noneMatched {
            NotFoundPage()
          }

          )
        )
      )
}

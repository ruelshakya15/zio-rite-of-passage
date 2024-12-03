package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import frontroute.* // for routing

import com.rockthejvm.reviewboard.pages.*

object Router {
  val externalUrlBus = EventBus[String]()

  def apply() =
    mainTag(
      onMountCallback(ctx =>
        externalUrlBus.events.foreach(url => dom.window.location.href = url)(ctx.owner)
      ),
      routes(
        div(
          cls := "container-fluid",
          // potential children
          (pathEnd | path("companies")) { // if localhost:1234 or localhost:1234/ OR // localhost:1234/companies
            CompaniesPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignUpPage()
          },
          path("changepassword") {
            ChangePasswordPage()
          },
          path("forgot") {
            ForgotPasswordPage()
          },
          path("recover") {
            RecoverPasswordPage()
          },
          path("logout") {
            LogoutPage()
          },
          path("profile") {
            ProfilePage()
          },
          path("post") {
            CreateCompanyPage()
          },
          path("company" / long) { companyId =>
            CompanyPage(companyId)
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}

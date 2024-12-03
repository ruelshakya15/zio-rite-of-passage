package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import frontroute.BrowserNavigation
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement

import com.rockthejvm.reviewboard.core.*

// JWT token security vulnerability explained in Logout chapter unit_5
case class LogoutPageState() extends FormState {
  override def errorList: List[Option[String]] = List()
  override def maybeSuccess: Option[String]    = None
  override def showStatus: Boolean             = false
}

object LogoutPage extends FormPage[LogoutPageState]("Logout") {

  override def basicState = LogoutPageState()

  override def renderChildren(): List[ReactiveHtmlElement[dom.html.Element]] = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "centered-text",
      "You've have been successfully logged out."
    )
  )
}

package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import frontroute.BrowserNavigation
import org.scalajs.dom

import zio.*

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import com.rockthejvm.reviewboard.components.Anchors

case class LoginFormState(
    email: String = "",
    password: String = "",
    upStreamError: Option[String] = None,
    override val showStatus: Boolean =  false 
    // NOTE***: you can override def -> val and also change it from data member to class argument
) extends FormState {
  private val userEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val passwordError: Option[String] =
    Option.when(password.isEmpty())("Password can't be empty")

  override val errorList                    = List(userEmailError, passwordError, upStreamError)
  override val maybeSuccess: Option[String] = None
}

object LoginPage extends FormPage[LoginFormState]("Login") {

  override def basicState = LoginFormState()
  
  val submitter = Observer[LoginFormState] { state =>
    // check stateVar errors -  if so, show errors in the panel
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      // if no errors, trigger the backend call
      useBackend(_.user.loginEndpoint(LoginRequest(state.email, state.password)))
        .map { userToken =>
          // if success, set the user token, navigate away
          Session.setUserState(userToken)
          stateVar.set(LoginFormState())
          BrowserNavigation.replaceState("/")
        } // if backend gave us an error, show that
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upStreamError = Some(e.getMessage())))
          }
        }
        .runJs
    }
  }

  def renderChildren() = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upStreamError = None)
        // upStreamError set to None to reset if user tries to enter again
    ),
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upStreamError = None)
    ),
    button(
      `type` := "button",
      "Log In", // prevenDefault to prevent form submit cause page refresh
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    Anchors.renderNavLink(
      "Forgot password?",
      "/forgot",
      "auth-link"
    )
  )

}

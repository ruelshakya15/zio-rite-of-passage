package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import zio.*

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.components.Anchors

case class RecoverPasswordState(
    email: String = "",
    token: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upStreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  def errorList: List[Option[String]] = List(
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid"),
    Option.when(token.isEmpty())("Token can't be empty"),
    Option.when(newPassword.isEmpty())("Password can't be empty"),
    Option.when(newPassword != confirmPassword)("Passwords must match")
  ) ++ upStreamStatus.map(_.left.toOption).toList

  def maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption)

}

object RecoverPasswordPage extends FormPage[RecoverPasswordState]("Reset Password") {

  override def basicState = RecoverPasswordState()
  override def renderChildren() = List(
    renderInput(
      "Your Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upStreamStatus = None)
      // upStreamError set to None to reset if user tries to enter again
    ),
    renderInput(
      "Recovery Token (from email)",
      "token-input",
      "text",
      true,
      "The token",
      (s, t) => s.copy(token = t, showStatus = false, upStreamStatus = None)
    ),
    renderInput(
      "New Password",
      "password-input",
      "password",
      true,
      "Your new password",
      (s, p) => s.copy(newPassword = p, showStatus = false, upStreamStatus = None)
    ),
    renderInput(
      "Confirm Password",
      "confirm-password-input",
      "password",
      true,
      "Confrim password",
      (s, p) => s.copy(confirmPassword = p, showStatus = false, upStreamStatus = None)
    ),
    button(
      `type` := "button",
      "Reset Password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    Anchors.renderNavLink(
      "Need a password recovery token?",
      "/forgot",
      "auth-link"
    )
  )

  val submitter = Observer[RecoverPasswordState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.recoverPasswordEndpoint(
          RecoverPasswordRequest(state.email, state.token, state.newPassword)
        )
      )
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upStreamStatus = Some(Right("Success! You can log in now."))
            )
          )
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upStreamStatus = Some(Left(e.getMessage()))))
          }
        }
        .runJs
    }
  }
}

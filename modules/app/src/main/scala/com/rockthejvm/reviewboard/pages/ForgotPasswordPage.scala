package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import zio.*

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.components.Anchors

case class ForgotPasswordState(
    email: String = "",
    upStreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override val errorList: List[Option[String]] =
    List(
      Option.when(!email.matches(Constants.emailRegex))("Email is invalid")
    ) ++ upStreamStatus.map(_.left.toOption).toList

  def maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption)
}

object ForgotPasswordPage extends FormPage[ForgotPasswordState]("Forgot Password") {

  override def basicState = ForgotPasswordState()
  override def renderChildren() = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upStreamStatus = None)
    ),
    button(
      `type` := "button",
      "Recover Password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    Anchors.renderNavLink(
      "Have a password recovery token?",
      "/recover",
      "auth-link"
    )
  )

  val submitter = Observer[ForgotPasswordState] { state =>
    // check stateVar errors -  if so, show errors in the panel
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.forgotPasswordEndpoint(ForgotPasswordRequest(state.email)))
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upStreamStatus = Some(Right("Check your email!"))
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

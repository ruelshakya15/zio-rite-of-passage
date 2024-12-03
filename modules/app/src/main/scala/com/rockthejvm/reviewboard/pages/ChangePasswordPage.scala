package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import org.scalajs.dom.html.Element
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.airstream.state.Var

import zio.*

import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*

case class ChangePasswordState(
    password: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upStreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override val errorList: List[Option[String]] = List(
    Option.when(password.isEmpty())("Password can't be empty"),
    Option.when(newPassword.isEmpty())("New password can't be empty"),
    Option.when(newPassword != confirmPassword)("Password must match")
  ) ++ upStreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption)
}

object ChangePasswordPage extends FormPage[ChangePasswordState]("Change Password") {

  override def basicState = ChangePasswordState()
  override def renderChildren(): List[ReactiveHtmlElement[Element]] = {
    Session.getUserState
      .map(_.email)
      .map(email =>
        List(
          renderInput(
            "Password",
            "password-input",
            "password",
            true,
            "Your password",
            (s, p) => s.copy(password = p, showStatus = false, upStreamStatus = None)
          ),
          renderInput(
            "New Password",
            "new-password-input",
            "password",
            true,
            "New password",
            (s, p) => s.copy(newPassword = p, showStatus = false, upStreamStatus = None)
          ),
          renderInput(
            "Confirm New Password",
            "confirm-password-input",
            "password",
            true,
            "Confirm password",
            (s, p) => s.copy(confirmPassword = p, showStatus = false, upStreamStatus = None)
          ),
          button(
            `type` := "button",
            "Change Password", // prevenDefault to prevent form submit cause page refresh
            onClick.preventDefault.mapTo(stateVar.now()) --> submitter(email)
          )
        )
      )
      .getOrElse(
        List(
          div(
            cls := "centered-text",
            "Ouch! It seems you are not logged in yet."
          )
        )
      )
  }

  def submitter(email: String) = Observer[ChangePasswordState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.updatePasswordEndpoint(
          UpdatePasswordRequest(email, state.password, state.newPassword)
        )
      )
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upStreamStatus = Some(Right("Password succesfully changed."))
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

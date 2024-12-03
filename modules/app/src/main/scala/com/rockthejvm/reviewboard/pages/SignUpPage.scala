package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import zio.*

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.*

case class SignUpPageFormState(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    upStreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  private val userEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val passwordError: Option[String] =
    Option.when(password.isEmpty())("Password can't be empty")
  private val confirmPasswordError: Option[String] =
    Option.when(password != confirmPassword)("Passwords must match")

  // these 2 below placed below or is would cause NullPointerException as some val are not initialized before
  override val errorList: List[Option[String]] =
    List(userEmailError, passwordError, confirmPasswordError) ++
      upStreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption)
}

object SignUpPage extends FormPage[SignUpPageFormState]("Sign Up") {

  override def basicState = SignUpPageFormState()

  override def renderChildren() = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upStreamStatus = None)
      // upStreamError set to None to reset if user tries to enter again
    ),
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upStreamStatus = None)
    ),
    renderInput(
      "Confirm Password",
      "confirm-password-input",
      "password",
      true,
      "Confirm password",
      (s, p) => s.copy(confirmPassword = p, showStatus = false, upStreamStatus = None)
    ),
    button(
      `type` := "button",
      "Sign Up", // prevenDefault to prevent form submit cause page refresh
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  val submitter = Observer[SignUpPageFormState] { state =>
    // check stateVar errors -  if so, show errors in the panel
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.createUserEndpoint(RegisterUserAccount(state.email, state.password)))
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upStreamStatus = Some(Right("Account created! You can login now."))
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

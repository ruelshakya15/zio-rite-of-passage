package com.rockthejvm.reviewboard.core

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom    // for local storage to keep track of user session
import scala.scalajs.js.* // to get javascript time
import com.rockthejvm.reviewboard.domain.data.UserToken

object Session {
  val stateName: String                 = "userState"
  val userState: Var[Option[UserToken]] = Var(Option.empty)

  def isActive = {
    loadUserState()
    userState.now().nonEmpty
  }

  def setUserState(token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set(stateName, token)
  }

  def loadUserState(): Unit = {
    // clears any expired token
    Storage
      .get[UserToken](stateName)
      .filter(
        _.expires * 1000 <= new Date().getTime()
      ) // js time (* 1000 to convert seconds to milliseconds as js gives time in ms)
      .foreach(_ => Storage.remove(stateName))

    // retrieve the user token (known to be valid)
    val currentToken = Storage.get[UserToken](stateName)
    if (currentToken != userState.now())
      userState.set(currentToken)
  }

  def clearUserState(): Unit =
    Storage.remove(stateName)
    userState.set(Option.empty)

  def getUserState: Option[UserToken] = {
    loadUserState()
    userState.now()
  }

}

package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import scala.scalajs.js.Date

object Footer {
  def apply() = div(
    cls := "main-footer",
    div(
        "Written in Scala with ❤️ by ",
        a(href := "https://www.linkedin.com/in/ruel-shakya-570ab319b/", "Ruel Shakya")
    ),
    div(s"©️ ${new Date().getFullYear()} all rights reserves")
  )
}

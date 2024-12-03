package com.rockthejvm.reviewboard.components

import scala.scalajs.*
import scala.scalajs.js.* // js.native APIS exposed
import scala.scalajs.js.annotation.*

@js.native
@JSImport("showdown", JSImport.Default)
object MarkdownLib extends js.Object { // class name DOESN'T matter
  @js.native
  class Converter extends js.Object { // class name DOES matter as it is inside a class
    def makeHtml(text: String): String = js.native
  }
}

// the API for the app
object Markdown {
  def toHtml(text: String) = new MarkdownLib.Converter().makeHtml(text)
}

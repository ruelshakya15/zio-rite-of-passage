package com.rockthejvm.reviewboard.components

import scala.scalajs.*
import scala.scalajs.js.* // js.native APIS exposed
import scala.scalajs.js.annotation.*

// import moment.js functionality
// moment library dependency is added to package.json

@js.native
@JSGlobal // accesible globally to js code that scalajs will generate
class Moment extends js.Object {
  def format(): String  = js.native
  def fromNow(): String = js.native
}

/* require "moment" in scalajs
    ; now we can moment.something(2) => String by defining as def something(arg: Int): String => js.native
    ; moment.unix(5234234) => Moment object (js object so we need to create a class for it)
    ; m.format('....') => "...."
 */
@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object {
  def unix(millis: Long): Moment = js.native

}

// API that I will use in the app
object Time {
  def unix2hr(millis: Long) =
    MomentLib.unix(millis / 1000).fromNow() // divide by 1000 as Moment takes seconds not millisecs
  
  def past(millis: Long) = new Date().getTime.toLong - millis

}

package com.rockthejvm.reviewboard

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import scala.util.Try
import com.raquo.airstream.ownership.OneTimeOwner

import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.core.*

import frontroute.LinkHandler

object App {

  val app = div(
    onMountCallback(_ => Session.loadUserState()),
    Header(),
    Router(),
    Footer()
  ).amend(LinkHandler.bind) // (modifier so app reacts to updated urls) for internal links

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(
      containerNode,
      app
    )
  }
}

// REACTIVE VARIABLEs
object Tutorial {
  val staticContent =
    div(
      // modifiers
      styleAttr := "color:red", // <div style="red">
      p("This is an app"),
      p("rock the JVM but also JS")
    )

  // EventStream - produce values of the same type
  val ticks = EventStream.periodic(1000) // EventStream[Int]
  // subscription - Airstream
  // ownership
  val subscription = ticks.addObserver(new Observer[Int] {
    override def onError(err: Throwable): Unit    = ()
    override def onTry(nextValue: Try[Int]): Unit = ()
    override def onNext(nextValue: Int): Unit     = dom.console.log(s"Ticks: $nextValue")
  })(new OneTimeOwner(() => ()))

  scala.scalajs.js.timers.setTimeout(10000)(subscription.kill())

  val timeUpdated =
    div(
      span("Time since loaded: "),
      child <-- ticks.map(number => s"$number seconds")
    )

  // EventBus - like EventStreams, but you can push new elements to the stream
  val clickEvents = EventBus[Int]()
  val clickUpdated = div(
    span("Clicks since loaded: "),
    child <-- clickEvents.events.scanLeft(0)(_ + _).map(number => s"$number clicks"),
    button(
      `type`    := "button", // need `` because type is keyword in scala
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    )
  )

  // Signal -  simlar to EventStream, but they have a  "current value" (state)
  // can be inspected for the current state (if Laminar/Airstream knows that it has an owner)
  val countSignal: OwnedSignal[Int] =
    clickEvents.events.scanLeft(0)(_ + _).observe(new OneTimeOwner(() => ()))
  val currentState = countSignal.now()
  val queryEvents  = EventBus[Unit]()

  val clickQueried = div(
    span("Clicks since loaded: "),
    child <-- queryEvents.events.map(_ => countSignal.now()),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    ),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.mapTo(()) --> queryEvents,
      "Refresh count"
    )
  )

  // Var - reactive variable
  val countVar = Var[Int](0) // need initial value
  val clicksVar = div(
    span("Clicks so far: "),
    child <-- countVar.signal.map(_.toString),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      "Add a click",
      // onClick --> countVar.updater((current, event) => current + 1)
      // onClick --> countVar.writer.contramap(event => countVar.now() + 1)
      onClick --> (_ => countVar.set(countVar.now() + 1))
    )
  )

  /** no state | with state
    * -------------------------------+--------------------- read EventStream | Signal
    * -------------------------------+--------------------- write EventBus | Var
    */

}

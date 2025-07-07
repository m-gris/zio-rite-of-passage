package com.rockthejvm.reviewboard.pages

import com.raquo.airstream.state.Var
import org.scalajs.dom.html.Element
import com. raquo. laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.core.Session

case class LogoutState() extends FormState {
  override def showStatus: Boolean = false
  override def maybeSuccess: Option[String] = None
  override def potentialErrors: List[Option[String]] = List()
}

object LogoutPage extends FormPage[LogoutState](title="Logout") {

  override val blankSlate = LogoutState()

  override val state = Var(blankSlate)

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(

    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "centered-text", // defined /declared in custom.css
      "You have been successfully logged out."
      )

    )

}

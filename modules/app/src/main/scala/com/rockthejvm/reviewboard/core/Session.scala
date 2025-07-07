package com.rockthejvm.reviewboard.core

import org.scalajs.dom
import com. raquo. laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.domain.data.UserSession
import scala.scalajs.js.Date

object Session {

  val stateName = "userState"

  val userState: Var[Option[UserSession]] = Var(Option.empty)

  def isActive: Boolean = {
    // WARNING: first -> reload the state to ensure token "freshness"
    loadUserState()
    userState.now().nonEmpty
  }

  def setUserState(session: UserSession): Unit = {
    userState.set(Option(session))
    Storage.set(
      stateName,
      session /*NOTA:
                  - serialization done in our Storage.set method
                  - AND ... our UserSession DERIVES JsonCodec
                  */
     )
  }

  def loadUserState(): Unit = {

    // clear any expired tokens
    Storage
      .get[UserSession](stateName)
      .filter(_.expires * 1000 < new Date().getTime())
      .foreach(_ => Storage.remove(stateName))

    // retrieve the 'known-to-be-still-valid' session
    userState.set(
      Storage.get[UserSession](stateName)
      )

  }

  def clearUserState(): Unit = {
    Storage.remove(stateName)
    userState.set(Option.empty)
  }

  def getUserState(): Option[UserSession] = {
    loadUserState() // load & clear out the UserSession/token if expired
    Storage.get(stateName)
  }


}

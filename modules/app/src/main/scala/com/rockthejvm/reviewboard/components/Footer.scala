package com.rockthejvm.reviewboard.components

import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.Date


object Footer {
  def apply() = div(
    cls := "main-footer",
    div(
      "Written in scala with 🩷",
      a(href := "https://rockthejvm.com", "at Rock The JVM")),
    div(s"© ${ new Date().getFullYear() } All rights reserved.")
  )
}

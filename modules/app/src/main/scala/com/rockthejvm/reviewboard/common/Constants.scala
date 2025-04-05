package com.rockthejvm.reviewboard.common

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {

  @js.native // bridges scala native strings to js native strings
  @JSImport("url:/static/img/fiery-lava 128x128.png", JSImport.Default)
  val logo: String = js.native

  @js.native
  @JSImport("/static/img/generic_company.png", JSImport.Default)
  val logoPlaceholder: String = js.native

}

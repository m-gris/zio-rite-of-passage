package com.rockthejvm.reviewboard.common

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {

  // WARNING: The "url:" prefix in @JSImport is crucial!
  // - WITH "url:": Parcel returns a plain string URL (e.g., "/path/to/image.hash.png")
  // - WITHOUT "url:": Parcel returns a module object (e.g., { default: "/path/to/image.hash.png" })
  // Without the "url:" prefix, using these values in string interpolation will cause:
  // java.lang.ClassCastException: object cannot be cast to java.lang.String
  // The values work fine in src attributes either way, but string operations require "url:"

  @js.native // bridges scala native strings to js native strings
  @JSImport("url:/static/img/fiery-lava 128x128.png", JSImport.Default)
  val logo: String = js.native

  @js.native
  @JSImport("url:/static/img/generic_company.png", JSImport.Default)
  val logoPlaceholder: String = js.native

  val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val urlRegex = """^(https?):\/\/(([^:/?#]+)(?::(\d+))?)(\/[^?#]*)?(\?[^#]*)?(#.*)?"""


}

package com.rockthejvm.reviewboard.components


import scala.scalajs.*
import scala.scalajs.js.* // js native
import scala.scalajs.js.annotation.* // will allow cross-compilation of scala code to JS
import com.rockthejvm.reviewboard.components.MarkdownLib.Converter

@js.native
@JSImport("showdown" /*markdown to html JS Lib*/, JSImport.Default)
object MarkdownLib extends js.Object {


  // for the Converter below, since the class is part of the MarkdownLib object,
  // CLASS NAME DOES MATTER,
  // i.e must match the javascript lib api
  @js.native
  class Converter extends js.Object {
    def makeHtml(text: String): String = js.native

  }
}

// the API for the app
object Markdown {
  def toHtml(text: String): String =
    new MarkdownLib.Converter().makeHtml(text)
}

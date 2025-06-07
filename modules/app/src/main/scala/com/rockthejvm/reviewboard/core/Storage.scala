package com.rockthejvm.reviewboard.core

import zio.json.*
import org.scalajs.dom

object Storage {

  def set[A](key: String, value: A)(using encoder: JsonEncoder[A]) =
    dom.window.localStorage.setItem(key, value.toJson /* .toJson ... EXTENSION METHOD FROM ZIO JSON USING THE IMPLICIT JsonEncoder... */)

  def get[A](key: String)(using decoder: JsonDecoder[A]): Option[A] =
    Option( // to handle null/undefined in Javascript
      dom.window.localStorage.getItem(key))
      .filter(!_.isEmpty) // we don't need/want empty strings
      .flatMap(value => decoder.decodeJson(value).toOption)

  def remove(key: String): Unit =
      dom.window.localStorage.removeItem(key)

}

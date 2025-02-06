package com.rockthejvm.reviewboard.domain.errors

import sttp.model.StatusCode

final case class HttpError (
  statusCode: StatusCode,
  message: String,
  cause: Throwable
) extends RuntimeException(message, cause)

object HttpError {

  def encode(tuple: (StatusCode, String)) =
    HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))

  def decode(error: Throwable) = (
    StatusCode.InternalServerError,
    error.getMessage) // standard JAVA API

}

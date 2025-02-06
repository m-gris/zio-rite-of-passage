package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

import com.rockthejvm.reviewboard.domain.errors.*

trait BaseEndpoint {

    val baseEndpoint =
      endpoint
        // describe the error type
        .errorOut(statusCode and plainBody[String]) // (StatusCode, String)
        // mapErrorOut defines bidirectional error type conversion
        .mapErrorOut[Throwable](HttpError.encode)(HttpError.decode)
}


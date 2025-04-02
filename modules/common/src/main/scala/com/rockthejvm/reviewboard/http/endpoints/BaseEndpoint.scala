package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

import com.rockthejvm.reviewboard.domain.errors.*

trait BaseEndpoint {

    val baseEndpoint =
      endpoint

        // DESCRIBE THE ERROR TYPE
        // Configure how errors are sent back to the client:
        // - statusCode: HTTP status code (e.g., 400, 404, 500)
        // - plainBody[String]: Error message as plain text
        // These two are combined using 'and' to create a tuple (StatusCode, String)
        .errorOut(statusCode and plainBody[String]) // (StatusCode, String)

        // BIDIRECTIONAL ERROR TYPE CONVERSION
        // Transform between application errors (Throwable) and HTTP responses
        // - First parameter (HttpError.encode): converts Throwable to (StatusCode, String)
        // - Second parameter (HttpError.decode): converts (StatusCode, String) back to Throwable
        // This bidirectional mapping ensures consistent error handling across the API
        .mapErrorOut[Throwable](HttpError.encode)(HttpError.decode)

    val securedBaseEndpoint =
      baseEndpoint
        // check for Header "Authorization: Bearer <token>"
        .securityIn(auth.bearer[String]())

}


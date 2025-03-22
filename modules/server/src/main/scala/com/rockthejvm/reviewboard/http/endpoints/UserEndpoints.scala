package com.rockthejvm.reviewboard.http.endpoints

// IMPORTS - 3rd parties
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation functionallity (to create the JsonCodecs)

// IMPORTS - Local
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.http.responses.*

trait UserEndpoints extends BaseEndpoint {

  // POST /users { email, password } →> { email }
  val userRegistrationEndpoint =
    baseEndpoint
      .tag("users")
      .name("register")
      .description("Register a user account with email & password")
      .in("users") // this defines the path segment of the URL, i.e this endpoint s accessible at the /users path.
      .post
      .in(jsonBody[UserRegistration])
      .out(jsonBody[UserResponse])

  // PUT /users/password { email, oldPassword, newPassword } →> { email }
  // TODO => Should AUTHORIZED with JWT !!!!!!
  val passwordUpdateEndpoint =
    securedBaseEndpoint
      .tag("users")
      .tag("password")
      .name("update password")
      .description("Update User Password")
      .in("users" / "password") // NB:  The / operator in Tapir composes URL path segments in a type-safe way, ensuring each segment is properly validated and encoded as part of the URL path.
      .put
      .in(jsonBody[PasswordUpdateRequest])
      .out(jsonBody[UserResponse])

  // DELETE /users ( email, password } →> { email }
  // TODO => Should AUTHORIZED with JWT !!!!!!
  val userDeletionEndpoint =
    securedBaseEndpoint
      .tag("users")
      .name("delete")
      .description("delete a user account")
      .in("users")
      .delete
      .in(jsonBody[UserDeletionRequest])
      .out(jsonBody[UserResponse])

  // POST /users/login { email, password } → { email, accessToken, expiration }
  val userLoginEndpoint =
    baseEndpoint
      .tag("users")
      .name("login")
      .description("log the user in")
      .in("users" / "login")
      .post
      .in(jsonBody[UserLoginRequest])
      .out(jsonBody[UserSession])
}

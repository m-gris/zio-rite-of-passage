package com.rockthejvm.reviewboard.repositories

import java.time.Instant

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.*

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  val joe = User(
    id = 1L,
    email = "joe@joemail.com",
    hashedPassword = "asldkfjslkjdf"
    )

  val jane = User(
    id = 2L,
    email = "jane@janemail.com",
    hashedPassword = "alsdfhqweu"
    )


  override val initScript: String = "sql/users.sql"

  override def spec: Spec[Environment & Scope, Any] =
    suite("UserRepositorySpec")(

      test("create user") {
        val program = for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(joe)
        } yield user

        program.assert { user =>

          user.id == joe.id &&
          user.email == joe.email &&
          user.hashedPassword == joe.hashedPassword

        }

      },


      test("get by ID") {
        for {
          repo                    <- ZIO.service[UserRepository]
          userCreated             <- repo.create(joe)
          userFeatchedById        <- repo.getById(userCreated.id)
        } yield assertTrue {
          userFeatchedById.contains(joe)
        }
      },

      test("get by EMAIL") {
        for {
          repo                      <- ZIO.service[UserRepository]
          userCreated               <- repo.create(joe)
          userFetchedByEmail        <- repo.getByEmail(userCreated.email)
        } yield assertTrue {
          userFetchedByEmail.contains(joe)
        }
      },


      test("update user") {
        for {
          repo            <- ZIO.service[UserRepository]
          createdUser     <- repo.create(joe)
          updatedUser     <- repo.update(createdUser.id, r => r.copy(email="joe@xxxxxx.com"))
        } yield assertTrue(
            createdUser.id == updatedUser.id &&
            updatedUser.email == "joe@xxxxxx.com"
          )
      },


      test("delete user") {
        for {
          repo        <- ZIO.service[UserRepository]
          user      <- repo.create(joe)
          _           <- repo.delete(user.id)
          mayBeUser <- repo.getById(user.id)
        } yield assertTrue(
                mayBeUser.isEmpty
              )
      },


    ).provide(
      UserRepositoryLive.layer,
      dataSourceLayer,
      Repository.dbLayer,
      Scope.default
    )

}


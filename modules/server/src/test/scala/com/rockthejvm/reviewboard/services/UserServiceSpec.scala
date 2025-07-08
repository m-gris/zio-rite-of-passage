package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.EmailService


object UserServiceSpec extends ZIOSpecDefault {

  val daniel = User(
    id=1L,
    email="daniel@rockthejvm.com",
    hashedPassword="1000:6BE6F282C8FEE210243A366E5F2C0DBE5752B13E0C88993C:D794642ECE64D1A564CC921C76CC83D9960FE8AEB0884D23"
  )

  val stubUserRepoLayer = ZLayer.succeed {

    new UserRepository {

      val db = collection.mutable.Map[Long, User](daniel.id -> daniel)

      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed {
        db.get(id)
      }

      override def getByEmail(email: String): Task[Option[User]] = ZIO.succeed {
        db.values.find(_.email == email)
      }

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val updatedUser = op(db(id)) // can fail... hence wrapped in an attempt
        db += (id -> updatedUser)
        updatedUser
      }

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }

    }

  }

  val stubJWTLayer = ZLayer.succeed {
    new JWTService {
      override def startSession(user: User): Task[UserSession] = ZIO.succeed {
        UserSession(daniel.id, daniel.email, "BIG ACCESS", Long.MaxValue)
      }

      override def verifyToken(token: String): Task[Identifiers] = ZIO.succeed {
        Identifiers(daniel.id, daniel.email)
      }
    }
  }

  val stubEmailServiceLayer = ZLayer.succeed {
    new EmailService {
      override def sendEmail(
        to: String,
        subject: String,
        content: String
      ): Task[Unit] = ZIO.unit // we just don't care sending emails here...
    }
  }

  val stubOTPRepoLayer = ZLayer.succeed {
    new OTPRepository {
      val db = collection.mutable.Map[String, String]()
      override def checkOTP(email: String, OTP: String): Task[Boolean] =
        ZIO.succeed(db.get(email).map(_ == OTP).nonEmpty)
      override def getOTP(email: String): Task[Option[String]] = ZIO.attempt {
        val otp: String = scala.util.Random.alphanumeric.take(8).mkString.toUpperCase()
        db += (email -> otp)
        Some(otp)
      }

    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =

    suite("UserServiceSpec") (

      test("Create & Validate a User") {
        for {
          userService <- ZIO.service[UserService]
          user        <- userService.registerUser(daniel.email, "rockthejvm")
          isValid     <- userService.verifyPassword(user.email, "rockthejvm")
        } yield assertTrue(isValid && user.email == daniel.email)
      },

      test("Validate Correct Credentials") {
        for {
          userService <- ZIO.service[UserService]
          user        <- userService.registerUser(daniel.email, "rockthejvm")
          isValid     <- userService.verifyPassword(user.email, "rockthejvm")
        } yield assertTrue(isValid && user.email == daniel.email)
      },

      test("Invalidate Credentials") {
        for {
          userService <- ZIO.service[UserService]
          user        <- userService.registerUser(daniel.email, "rockthejvm")
          isValid     <- userService.verifyPassword(user.email, "flopthejvm")
        } yield assertTrue(!isValid)
      },

      test("Invalidate Non-Existant User") {
        for {
          userService <- ZIO.service[UserService]
          isValid     <- userService.verifyPassword("someone@gmail.com", "flopthejvm")
        } yield assertTrue(!isValid)
      },

      test("Update Password") {
        for {
          userService     <- ZIO.service[UserService]
          updatedUser     <- userService.updatePassword(daniel.email, "rockthejvm", "scalarulez")
          oldIsValid      <- userService.verifyPassword(daniel.email, "rockthejvm")
          newIsValid      <- userService.verifyPassword(daniel.email, "scalarulez")
        } yield assertTrue(newIsValid && !oldIsValid)
      },

      test("Delete User") {
        for {
          userService     <- ZIO.service[UserService]
          deletedUser     <- userService.deleteUser(daniel.email, "rockthejvm")
          error           <- userService.login(daniel.email, "rockthejvm").flip
        } yield assertTrue(
                deletedUser.email == daniel.email &&
                error.isInstanceOf[RuntimeException])
      },

      test("Delete Non-Existing User Should Fail") {
        for {
          userService     <- ZIO.service[UserService]
          error           <- userService
                              .deleteUser("someone@nowhere.com", "1234")
                              .flip /* flip the exception as the value channel of the zio */
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },

      test("Delete With Wrong Credentials Should Fail") {
        for {
          userService     <- ZIO.service[UserService]
          error           <- userService
                              .deleteUser(daniel.email, "wrong password")
                              .flip /* flip the exception as the value channel of the zio */
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },

      ).provide(
        stubJWTLayer,
        stubOTPRepoLayer,
        stubUserRepoLayer,
        stubEmailServiceLayer,
        UserServiceLive.layer,
        )


}

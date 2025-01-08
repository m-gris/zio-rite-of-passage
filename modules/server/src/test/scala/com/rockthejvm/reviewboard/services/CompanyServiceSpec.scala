package com.rockthejvm.reviewboard.services

import scala.collection.mutable

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.domain.data.Company

object CompanyServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CompanyService]

  val CompanyRepoStubLayer = ZLayer.succeed (

    new CompanyRepository {

      val db = mutable.Map[Long, Company]()

      override def create(company: Company): Task[Company] =
        ZIO.succeed {
          val nextId = db.keys.maxOption.getOrElse(0L) + 1
          val newCompany = company.copy(id=nextId)
          db += (nextId -> newCompany)
          newCompany
        }

      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val currentCompany = db(id) // can crash
          val updatedCompany = op(currentCompany)
          db += (id -> updatedCompany)
          updatedCompany
        }

      override def delete(id: Long): Task[Company] =
        ZIO.attempt {
          val company = db(id) // can crash
          db -= id
          company
        }

      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def getAll: Task[List[Company]] =
        ZIO.succeed(db.values.toList)

    }

  )

  override def spec: Spec[TestEnvironment & Scope, Any] =

    suite("CompanyServiceSpec")(

      test("create"){

        val companyZIO = service(_.create(CompanyCreationRequest(name="Rock The JVM", url="rockthejvm.com")))

        companyZIO.assert { company =>
          company.name == "Rock The JVM" &&
          company.slug == "rock-the-jvm" &&
          company.url == "rockthejvm.com"
        }

      },

      test("get by ID") {

        val both = for {

          createdCompany <- service(_.create(CompanyCreationRequest(name="Rock The JVM", url="rockthejvm.com")))

          retrievedCompany <- service(_.getById(createdCompany.id))

        } yield (createdCompany, retrievedCompany)

        both.assert {
          case (createdCompany, Some(retrievedCompany)) =>
            createdCompany == retrievedCompany
          case _ => false
          }
        },

      test("get by Slug") {

        val both = for {
          created   <- service(_.create(CompanyCreationRequest(name="Rock The JVM", url="rockthejvm.com")))
          retrieved <- service(_.getBySlug(created.slug))
        } yield (created, retrieved)

        both.assert {
          case (created, Some(retrieved)) => created == retrieved
          case _ => false
        }
      },

      test("get all companies") {
        val program = for {
          company1    <- service(_.create(CompanyCreationRequest(name="Rock The JVM", url="rockthejvm.com")))
          company2   <- service(_.create(CompanyCreationRequest(name="Crash The JVM", url="crashthejvm.com")))
          companies <- service(_.getAll)
        } yield (company1, company2, companies)


        program.assert {
          case (c1, c2, companies) => companies.toSet == Set(c1, c2)
        }
      }

      )
        .provide(
          CompanyServiceLive.layer,
          CompanyRepoStubLayer ,
          )

}
